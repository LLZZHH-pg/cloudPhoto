package com.lab.study.userservice.service;

import com.lab.study.userservice.entity.User;
import java.util.Map;

/**
 * 用户服务接口
 * 定义了用户注册、登录及信息查询的标准方法
 */
public interface UserService {

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 包含 Token 和用户信息的 Map
     */
    Map<String, Object> login(String username, String password);

    /**
     * 用户注册
     * @param user 用户实体
     */
    void register(User user);

    /**
     * 根据 ID 获取用户信息
     * @param userId 用户 ID
     * @return 用户实体
     */
    User getUserById(Integer userId);
}