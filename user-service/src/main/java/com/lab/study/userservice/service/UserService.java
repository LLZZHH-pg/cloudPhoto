package com.lab.study.userservice.service;

import com.LAB.study.dto.RegisterDTO;
import com.LAB.study.dto.UserInfoDTO;
import com.LAB.study.vo.PlanVO;

import java.util.List;
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

    void updateRegister(RegisterDTO dto, Integer integer);

    List<PlanVO> getAllPlans();

    void subscribePlan(Integer userId, Integer planId);
}