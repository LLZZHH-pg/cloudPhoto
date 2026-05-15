package com.lab.study.albummanageservice.dto;

import lombok.Data;

import java.util.List;

/**
 * 影集添加照片请求体
 */
@Data
public class AlbumPhotoRequest {

    /** 要添加或移除的照片ID列表 */
    private List<Long> photoIds;
}
