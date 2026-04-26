package com.lab.study.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.study.userservice.entity.User;
import com.lab.study.userservice.mapper.UserMapper;
import com.lab.study.userservice.service.UserService;
import com.lab.study.userservice.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 * 实现了 UserService 接口的具体逻辑
 */
@Service // 注意：注解打在实现类上
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Map<String, Object> login(String username, String password) {
        // 1. 查询用户
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("nam", username);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 校验密码 (使用 BCrypt)
        if (!passwordEncoder.matches(password, user.getPas())) {
            throw new RuntimeException("密码错误");
        }

        // 3. 生成 JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("username", user.getNam());
        String token = JwtUtil.createJWT("cloud-photo-key", 3600000L, claims);

        // 4. 存入 Redis
        //String redisKey = "login:token:" + user.getUserId();
        //redisTemplate.opsForValue().set(redisKey, token, 60, TimeUnit.MINUTES);

        // 5. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", user);
        return result;
    }

    @Override
    public void register(User user) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("nam", user.getNam());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        // 密码加密
        user.setPas(passwordEncoder.encode(user.getPas()));
        // 默认配额 1GB
        user.setTotalstorage(1024L * 1024 * 1024);
        user.setUsedstorage(0L);

        userMapper.insert(user);
    }

    @Override
    public User getUserById(Integer userId) {
        return userMapper.selectById(userId);
    }
}