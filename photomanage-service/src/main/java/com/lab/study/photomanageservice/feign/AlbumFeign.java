package com.lab.study.photomanageservice.feign;


import com.LAB.study.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "albummanage-service")
public interface AlbumFeign {
    @DeleteMapping("/album/internal/photos/clear")
    Result<Void> clearPhotosFromAlbums(@RequestBody List<Long> photoIds);
}
