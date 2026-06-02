package com.LAB.study.request;

import lombok.Data;

import java.util.List;

/**
 * 复制照片请求体
 */
@Data
public class CopyPhotoRequest {

    /** 源影集ID */
    private Long sourceAlbumId;

    /** 要复制的照片ID列表 */
    private List<Long> photoIds;

    /** 目标影集ID列表 */
    private List<Long> targetAlbumIds;
}
