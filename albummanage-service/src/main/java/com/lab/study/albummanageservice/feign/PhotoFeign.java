package com.lab.study.albummanageservice.feign;

import com.LAB.study.dto.PictureDTO;
import com.LAB.study.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "photomanage-service")
public interface PhotoFeign {

    @PostMapping("/media/internal/getphoto")
    Result<List<PictureDTO>> getPicturesByIds(@RequestBody List<Long> ids);
}
