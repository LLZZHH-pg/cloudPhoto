package com.LAB.study.dto;

import lombok.Data;

import java.util.List;

/**
 * 移动照片请求体
 */
@Data
public class MovePhotoRequest {

    /** 源影集ID */
    private Long sourceAlbumId;

    /** 要移动的照片ID列表 */
    private List<Long> photoIds;

    /** 目标影集ID */
    private Long targetAlbumId;
}
