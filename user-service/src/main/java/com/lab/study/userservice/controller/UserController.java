package com.lab.study.userservice.controller;

import com.LAB.study.dto.LoginDTO;
import com.LAB.study.dto.RegisterDTO;
import com.LAB.study.result.Result;
import com.lab.study.userservice.entity.User;
import com.lab.study.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    
    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO dto) {
        try {
            Map<String, Object> data = userService.login(dto.getAcc(), dto.getPas());
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 注册接口
     */
    @PostMapping("/register")
    public Result register(@RequestBody RegisterDTO dto) {
        try {
            userService.register(dto);
            return Result.success("注册成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 内部接口：根据ID获取用户信息
     * 供网关验证Token后查询用户详情
     */
    @GetMapping("/info/{id}")
    public Result<User> getUserInfo(@PathVariable Integer id) {
        User user = userService.getUserById(id);
        if (user != null) {
            return Result.success(user);
        } else {
            return Result.error("用户不存在");
        }
    }
}