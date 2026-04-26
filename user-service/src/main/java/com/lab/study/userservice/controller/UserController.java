package com.lab.study.userservice.controller;

import com.lab.study.userservice.entity.User;
import com.lab.study.userservice.service.UserService;
import com.lab.study.userservice.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 登录接口
     * POST /user/login
     */
    @PostMapping("/login")
    public ResultVo login(@RequestBody User user) {
        try {
            Map<String, Object> data = userService.login(user.getNam(), user.getPas());
            return ResultVo.success(data);
        } catch (Exception e) {
            return ResultVo.fail(e.getMessage());
        }
    }

    /**
     * 注册接口
     */
    @PostMapping("/register")
    public ResultVo register(@RequestBody User user) {
        try {
            userService.register(user);
            return ResultVo.success("注册成功");
        } catch (Exception e) {
            return ResultVo.fail(e.getMessage());
        }
    }

    /**
     * 内部接口：根据ID获取用户信息
     * 供网关验证Token后查询用户详情
     */
    @GetMapping("/info/{id}")
    public ResultVo<User> getUserInfo(@PathVariable Integer id) {
        User user = userService.getUserById(id);
        if (user != null) {
            return ResultVo.success(user);
        } else {
            return ResultVo.fail("用户不存在");
        }
    }
}