package com.lab.study.albummanageservice.feign;

import com.LAB.study.dto.PictureDTO;
import com.LAB.study.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "photomanage-service")
public interface PhotoFeign {

    @PostMapping("/media/internal/getphoto")
    Result<List<PictureDTO>> getPicturesByIds(@RequestBody List<Long> ids);

    @GetMapping("/media/internal/category/first")
    Result<Map<String, List<PictureDTO>>> getCategoryFirstGroup(@RequestParam("userId") Integer userId);

    @GetMapping("/media/internal/category/all")
    Result<Map<String, List<PictureDTO>>> getCategoryAllGroup(@RequestParam("userId") Integer userId);
}
