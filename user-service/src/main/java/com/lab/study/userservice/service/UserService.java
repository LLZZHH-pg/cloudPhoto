package com.lab.study.userservice.service;

import com.LAB.study.dto.RegisterDTO;
import com.LAB.study.dto.UserInfoDTO;
import com.lab.study.userservice.entity.User;
import java.util.Map;

/**
 * 用户服务接口
 * 定义了用户注册、登录及信息查询的标准方法
 */
public interface UserService {

    Map<String, Object> login(String account, String password);

    void register(RegisterDTO dto);

    UserInfoDTO getUserById(Integer userId);

    void updateUsedStorage(Integer userId, Long sizeDelta);
}