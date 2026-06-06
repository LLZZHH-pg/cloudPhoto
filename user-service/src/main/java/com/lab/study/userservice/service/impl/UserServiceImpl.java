package com.lab.study.userservice.service.impl;

import com.LAB.study.dto.RegisterDTO;
import com.LAB.study.dto.UserInfoDTO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.lab.study.userservice.entity.User;
import com.lab.study.userservice.mapper.UserMapper;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl implements UserService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Autowired
    private UserMapper userMapper;

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

        // 赋值与插入
        User user = new User();
        user.setNam(nam);
        user.setPas(passwordEncoder.encode(pas));
        user.setTel(tel);
        user.setEml(eml);
        user.setUsedstorage(0L);

        userMapper.insert(user);
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
    public UserInfoDTO getUserById(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return null;

        UserInfoDTO dto = new UserInfoDTO();
        BeanUtils.copyProperties(user, dto);

        Long total = userMapper.getRemainingStorage(userId);
        dto.setTotalstorage(total);

        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUsedStorage(Integer userId, Long sizeDelta) {
        if (sizeDelta == 0) {
            return;
        }

        if (sizeDelta > 0) {
            // 需要增加使用容量时（如上传照片）
            int updated = userMapper.deductStorage(userId, sizeDelta);
            if (updated == 0) {
                throw new RuntimeException("操作失败，您的存储空间不足，请清理后重试");
            }
        } else {
            // 需要释放使用容量时（如彻底清理回收站，传入负数）
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
                    .ne(User::getUserId, excludeUserId)); // 核心：增加“不等于当前用户ID”的条件
            if (count > 0) {
                throw new RuntimeException(msg);
            }
        }
    }


}