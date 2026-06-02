package com.LAB.study.request;

import lombok.Data;

import java.util.List;

/**
 * 影集添加照片请求体
 */
@Data
public class AlbumPhotoRequest {

    /** 源影集ID（可为空，照片可能还未加入任何影集） */
    private Long sourceAlbumId;

    /** 影集ID */
    private Long albumId;

    /** 要添加或移除的照片ID列表 */
    private List<Long> photoIds;
}
