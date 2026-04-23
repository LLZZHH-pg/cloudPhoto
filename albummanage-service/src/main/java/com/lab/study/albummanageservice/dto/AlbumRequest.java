package com.lab.study.albummanageservice.dto;

import lombok.Data;

/**
 * 创建/更新影集请求体
 */
@Data
public class AlbumRequest {

    /** 影集名称（必填） */
    private String name;

    /** 影集描述 */
    private String description;

    /** 封面图片URL */
    private String coverUrl;

    /** 是否公开：0-私有，1-公开，默认0 */
    private Integer isPublic = 0;
}
