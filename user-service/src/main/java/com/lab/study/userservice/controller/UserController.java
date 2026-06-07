package com.lab.study.userservice.controller;

import com.LAB.study.context.UserContextHolder;
import com.LAB.study.dto.LoginDTO;
import com.LAB.study.dto.RegisterDTO;
import com.LAB.study.dto.UserInfoDTO;
import com.LAB.study.request.PlanRequest;
import com.LAB.study.request.UserStatusRequest;
import com.LAB.study.result.Result;
import com.LAB.study.vo.PlanVO;
import com.lab.study.userservice.entity.User;
import com.lab.study.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    private Integer currentUserId() {
        return UserContextHolder.getCurrentUserId();
    }
    
    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO dto) {
        try {
            Map<String, Object> data = userService.login(dto.getAcc(), dto.getPas());
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result register(@RequestBody RegisterDTO dto) {
        try {
            userService.register(dto);
            return Result.success("注册成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/info")
    public Result<UserInfoDTO> getUserInfo() {
        UserInfoDTO dto = userService.getUserById(currentUserId());
        if (dto != null) {
            return Result.success(dto);
        } else {
            return Result.error("用户不存在");
        }
    }

    @PostMapping("/info/update")
    public Result updateUserInfo(@RequestBody RegisterDTO dto) {
        try {
            userService.updateRegister(dto,currentUserId());
            return Result.success("修改成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/plans/list")
    public Result<List<PlanVO>> getPlans() {
        currentUserId();
        return Result.success(userService.getAllPlans());
    }

    @PostMapping("/plans/subscribe")
    public Result subscribe(@RequestBody Map<String, Integer> params) {
        Integer planId = params.get("planId");
        try {
            userService.subscribePlan(currentUserId(), planId);
            return Result.success("订阅成功");
        } catch (Exception e) {
            return Result.error("订阅失败");
        }
    }

    //管理员
    @PostMapping("/auth/login")
    public Result loginForAuth(@RequestBody LoginDTO dto) {
        try {
            Map<String, Object> data = userService.loginForAuth(dto.getAcc(), dto.getPas());
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/auth/plans/list")
    public Result<List<PlanVO>> getPlansForAuth() {
        try {
            checkAdminAuth();
            List<PlanVO> list = userService.getAllPlansForAuth();
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/auth/plans/save")
    public Result savePlan(@RequestBody PlanRequest request) {
        try {
            checkAdminAuth();
            userService.saveOrUpdatePlan(request);
            return Result.success("操作成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/auth/plans/delete")
    public Result deletePlan(@RequestBody PlanRequest request) {
        try {
            checkAdminAuth();
            if (request.getPlanid() == null) return Result.error("请提供planid");
            userService.deletePlan(request.getPlanid());
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/auth/user/status")
    public Result updateUserStatus(@RequestBody UserStatusRequest request) {
        try {
            checkAdminAuth();
            userService.updateUserStatus(request);
            return Result.success("状态修改成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private void checkAdminAuth() {
        UserInfoDTO user = userService.getUserById(currentUserId());
        if (user == null || !"auth".equals(user.getStatues())) {
            throw new RuntimeException("无权访问管理员功能");
        }
    }


    //内部
    @GetMapping("/internal/info/{id}")
    public Result<UserInfoDTO> getUserInfo(@PathVariable Integer id) {
        UserInfoDTO dto = userService.getUserById(id);
        if (dto != null) {
            return Result.success(dto);
        } else {
            return Result.error("用户不存在");
        }
    }

    @PostMapping("/internal/storage/update")
    public Result<Void> updateStorage(@RequestParam("userId") Integer userId, @RequestParam("sizeDelta") Long sizeDelta) {
        try {
            userService.updateUsedStorage(userId, sizeDelta);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}