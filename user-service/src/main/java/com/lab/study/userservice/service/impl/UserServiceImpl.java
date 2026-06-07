package com.lab.study.userservice.service.impl;

import com.LAB.study.dto.RegisterDTO;
import com.LAB.study.dto.UserInfoDTO;
import com.LAB.study.request.PlanRequest;
import com.LAB.study.request.UserStatusRequest;
import com.LAB.study.vo.PlanVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.lab.study.userservice.entity.Plan;
import com.lab.study.userservice.entity.User;
import com.lab.study.userservice.entity.UserPlan;
import com.lab.study.userservice.mapper.PlanMapper;
import com.lab.study.userservice.mapper.UserMapper;
import com.lab.study.userservice.mapper.UserPlanMapper;
import com.lab.study.userservice.service.UserService;
import com.lab.study.userservice.utils.JwtUtil;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PlanMapper planMapper;
    @Autowired
    private UserPlanMapper userPlanMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 预编译正则表达式
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern NUMBER_ONLY_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\u4e00-\\u9fa5]+$"); // 无符号：只能中英数
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    @Override
    public Map<String, Object> login(String account, String password) {
        if (account == null || account.trim().isEmpty()) {
            throw new RuntimeException("登录账号不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        // 判断账号类型 (邮箱 / 手机号 / 用户名)
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(account.contains("@"), User::getEml, account)
                .eq(PHONE_PATTERN.matcher(account).matches(), User::getTel, account)
                .eq(!account.contains("@") && !PHONE_PATTERN.matcher(account).matches(), User::getNam, account));

        if (user == null) {
            throw new RuntimeException("该账号不存在，请检查输入或先注册");
        }

        if ("disable".equalsIgnoreCase(user.getStatues())) {
            throw new RuntimeException("该账号已被禁用，请联系管理员");
        }

        // 校验密码
        if (!passwordEncoder.matches(password, user.getPas())) {
            throw new RuntimeException("密码错误，请重试");
        }

        // 1. 生成唯一的 jti
        String jti = java.util.UUID.randomUUID().toString();

        // 2. 构造专业的 JwtUserDTO
        com.LAB.study.dto.JwtUserDTO jwtUser = new com.LAB.study.dto.JwtUserDTO();
        jwtUser.setUid(user.getUserId());

        // 3. 构建 Claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("user", jwtUser);
        claims.put("jti", jti);

        // 4. 生成 JWT
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        String token = JwtUtil.createJWT(key, "cloud-photo-key", jwtExpiration, claims);

        // 5. 存入 Redis 并返回
        String redisKey = "auth:token:" + jti;
        redisTemplate.opsForValue().set(redisKey, token, jwtExpiration, TimeUnit.MILLISECONDS);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", user);
        return result;
    }

    @Override
    public void register(RegisterDTO dto) {
        String nam = dto.getNam() == null ? "" : dto.getNam().trim();
        String pas = dto.getPas() == null ? "" : dto.getPas().trim();
        String tel = dto.getTel() == null ? "" : dto.getTel().trim();
        String eml = dto.getEml() == null ? "" : dto.getEml().trim();

        // 空值校验
        if (tel.isEmpty()) {
            throw new RuntimeException("号码不能为空");
        }
        if (pas.isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }
        // 密码校验
        if (pas.length() < 6) {
            throw new RuntimeException("密码长度不能少于6位");
        }
        // 电话格式校验
        if (!PHONE_PATTERN.matcher(tel).matches()) {
            throw new RuntimeException("电话号码必须是11位纯数字");

        }
        checkUnique(User::getTel, tel, "手机号已存在");
        // 用户名校验
        if (!nam.isEmpty()) {
            if (nam.length() < 2) {
                throw new RuntimeException("用户名不能少于2个字符");
            }
            if (NUMBER_ONLY_PATTERN.matcher(nam).matches()) {
                throw new RuntimeException("用户名不能是纯数字");
            }
            if (!USERNAME_PATTERN.matcher(nam).matches()) {
                throw new RuntimeException("用户名只能包含中英文和数字，不能使用符号");
            }
            checkUnique(User::getNam, nam, "用户名已存在");
        }
        // 邮箱格式校验
        if (!eml.isEmpty()) {
            if (!EMAIL_PATTERN.matcher(eml).matches()) {
                throw new RuntimeException("邮箱格式不正确");
            }
            checkUnique(User::getEml, eml, "邮箱已存在");
        }

        User user = new User();
        user.setNam(nam);
        user.setPas(passwordEncoder.encode(pas));
        user.setTel(tel);
        user.setEml(eml);
        user.setUsedstorage(0L);

        userMapper.insert(user);

        // 默认订阅 planid 为 1 的免费套餐
        UserPlan defaultPlan = new UserPlan();
        defaultPlan.setUserid(user.getUserId());
        defaultPlan.setPlanid(1);
        defaultPlan.setCreatedAt(LocalDateTime.now());

        userPlanMapper.insert(defaultPlan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRegister(RegisterDTO dto,Integer userId) {

        String nam = dto.getNam() == null ? "" : dto.getNam().trim();
        String pas = dto.getPas() == null ? "" : dto.getPas().trim();
        String tel = dto.getTel() == null ? "" : dto.getTel().trim();
        String eml = dto.getEml() == null ? "" : dto.getEml().trim();

        User user = new User();
        user.setUserId(userId);

        // 密码校验
        if (!pas.isEmpty()) {
            if (pas.length() < 6) {
                throw new RuntimeException("密码长度不能少于6位");
            }

            user.setPas(passwordEncoder.encode(pas));
        }
        // 电话格式校验
        if (!tel.isEmpty()) {

            if (!PHONE_PATTERN.matcher(tel).matches()) {
                throw new RuntimeException("电话号码必须是11位纯数字");
            }
            checkUniqueExceptSelf(User::getTel, tel,userId, "手机号已存在");

            user.setTel(tel);
        }
        // 用户名校验
        if (!nam.isEmpty()) {
            if (nam.length() < 2) {
                throw new RuntimeException("用户名不能少于2个字符");
            }
            if (NUMBER_ONLY_PATTERN.matcher(nam).matches()) {
                throw new RuntimeException("用户名不能是纯数字");
            }
            if (!USERNAME_PATTERN.matcher(nam).matches()) {
                throw new RuntimeException("用户名只能包含中英文和数字，不能使用符号");
            }
            checkUniqueExceptSelf(User::getNam, nam,userId, "用户名已存在");

            user.setNam(nam);
        }
        // 邮箱格式校验
        if (!eml.isEmpty()) {
            if (!EMAIL_PATTERN.matcher(eml).matches()) {
                throw new RuntimeException("邮箱格式不正确");
            }
            checkUniqueExceptSelf(User::getEml, eml,userId, "邮箱已存在");

            user.setEml(eml);
        }

        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUsedStorage(Integer userId, Long sizeDelta) {
        if (sizeDelta == 0) {
            return;
        }

        if (sizeDelta > 0) {
            // 增加容量
            int updated = userMapper.deductStorage(userId, sizeDelta);
            if (updated == 0) {
                throw new RuntimeException("操作失败，您的存储空间不足，请清理后重试");
            }
        } else {
            // 释放容量
            userMapper.releaseStorage(userId, Math.abs(sizeDelta));
        }
    }

    private void checkUnique(SFunction<User, ?> column, String value, String msg) {
        if (StringUtils.hasText(value) && userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(column, value)) > 0) {
            throw new RuntimeException(msg);
        }
    }

    private void checkUniqueExceptSelf(SFunction<User, ?> column, String value, Integer excludeUserId, String msg) {
        if (StringUtils.hasText(value)) {
            long count = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                    .eq(column, value)
                    .ne(User::getUserId, excludeUserId));
            if (count > 0) {
                throw new RuntimeException(msg);
            }
        }
    }

    @Override
    public List<PlanVO> getAllPlans() {
        List<Plan> plans = planMapper.selectList(Wrappers.<Plan>lambdaQuery()
                .eq(Plan::getStatues, "enable"));
        return plans.stream().map(p -> {
            PlanVO vo = new PlanVO();
            BeanUtils.copyProperties(p, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<PlanVO> getAllPlansForAuth() {
        List<Plan> plans = planMapper.selectList(null);
        return plans.stream().map(p -> {
            PlanVO vo = new PlanVO();
            BeanUtils.copyProperties(p, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void subscribePlan(Integer userId, Integer planId) {
        // 检查套餐是否存在且启用
        Plan plan = planMapper.selectById(planId);
        if (plan == null || !"enable".equals(plan.getStatues())) {
            throw new RuntimeException("该套餐目前不可用");
        }

        // 修改或插入关联记录（假设一个用户只能有一个激活套餐）
        UserPlan up = userPlanMapper.selectOne(Wrappers.<UserPlan>lambdaQuery().eq(UserPlan::getUserid, userId));
        if (up == null) {
            up = new UserPlan();
            up.setUserid(userId);
            up.setPlanid(planId);
            up.setCreatedAt(LocalDateTime.now());
            userPlanMapper.insert(up);
        } else {
            up.setPlanid(planId);
            up.setCreatedAt(LocalDateTime.now());
            userPlanMapper.updateById(up);
        }
    }

    //管理员
    @Override
    public Map<String, Object> loginForAuth(String account, String password) {
        if (account == null || account.trim().isEmpty()) {
            throw new RuntimeException("登录账号不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        // 判断账号类型 (邮箱 / 手机号 / 用户名)
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(account.contains("@"), User::getEml, account)
                .eq(PHONE_PATTERN.matcher(account).matches(), User::getTel, account)
                .eq(!account.contains("@") && !PHONE_PATTERN.matcher(account).matches(), User::getNam, account));

        if (user == null) {
            throw new RuntimeException("该账号不存在，请检查输入或先注册");
        }

        if (!"auth".equalsIgnoreCase(user.getStatues())) {
            throw new RuntimeException("非管理员账户");
        }

        // 校验密码
        if (!passwordEncoder.matches(password, user.getPas())) {
            throw new RuntimeException("密码错误，请重试");
        }

        // 1. 生成唯一的 jti
        String jti = java.util.UUID.randomUUID().toString();

        // 2. 构造专业的 JwtUserDTO
        com.LAB.study.dto.JwtUserDTO jwtUser = new com.LAB.study.dto.JwtUserDTO();
        jwtUser.setUid(user.getUserId());

        // 3. 构建 Claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("user", jwtUser);
        claims.put("jti", jti);

        // 4. 生成 JWT
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        String token = JwtUtil.createJWT(key, "cloud-photo-key", jwtExpiration, claims);

        // 5. 存入 Redis 并返回
        String redisKey = "auth:token:" + jti;
        redisTemplate.opsForValue().set(redisKey, token, jwtExpiration, TimeUnit.MILLISECONDS);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", user);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdatePlan(PlanRequest request) {
        if (request.getPlanid() == null) {
            // 校验必填项不允许为空
            if (!StringUtils.hasText(request.getName()) ||
                    request.getStorage() == null ||
                    request.getRecycle() == null ||
                    request.getPrice() == null ||
                    request.getStatues() == null) {
                throw new RuntimeException("属性值不能为空");
            }

            Plan plan = new Plan();
            BeanUtils.copyProperties(request, plan);
            planMapper.insert(plan);
        } else {
            // 直接构造 Plan 对象，MyBatis-Plus 的 updateById 默认只更新非 null 字段
            Plan plan = new Plan();
            BeanUtils.copyProperties(request, plan);

            int rows = planMapper.updateById(plan);
            if (rows == 0) {
                throw new RuntimeException("未找到对应套餐");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Integer planId) {
        if (planId == 1) throw new RuntimeException("默认免费套餐不能删除");
        planMapper.deleteById(planId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(UserStatusRequest request) {
        String status = request.getStatus();
        if (!List.of("enable", "disable", "auth").contains(status)) {
            throw new RuntimeException("非法的状态值");
        }
        User user = new User();
        user.setUserId(request.getUserId());
        user.setStatues(status);
        userMapper.updateById(user);
    }


    //内部
    @Override
    public UserInfoDTO getUserById(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return null;

        UserInfoDTO dto = new UserInfoDTO();
        BeanUtils.copyProperties(user, dto);

        Long total = userMapper.getTotalStorage(userId);
        Integer recycle = userMapper.getRecycleDays(userId);
        dto.setTotalstorage(total);
        dto.setRecycledays(recycle);

        return dto;
    }
}