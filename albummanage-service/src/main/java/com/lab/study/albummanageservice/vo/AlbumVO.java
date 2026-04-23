package com.lab.study.albummanageservice.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 影集详情响应
 */
@Data
public class AlbumVO {

    private Long id;
    private String name;
    private String description;
    private String coverUrl;
    private Long userId;
    private Integer isPublic;
    private Integer photoCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 影集内的照片ID列表（详情接口返回） */
    private List<Long> photoIds;
}
