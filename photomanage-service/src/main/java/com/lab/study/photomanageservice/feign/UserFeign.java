package com.lab.study.photomanageservice.feign;

import com.LAB.study.dto.UserInfoDTO;
import com.LAB.study.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service")
public interface UserFeign {

    @GetMapping("/user/info/{id}")
    Result<UserInfoDTO> getUserInfo(@PathVariable("id") Integer id);

    @PostMapping("/user/internal/storage/update")
    Result<Void> updateStorage(@RequestParam("userId") Integer userId, @RequestParam("sizeDelta") Long sizeDelta);
}
