package com.lab.study.albummanageservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 影集实体
 */
@Data
@TableName("album")
public class Album {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 影集名称 */
    private String name;

    /** 影集描述 */
    private String description;

    /** 封面图片URL */
    private String coverUrl;

    /** 所属用户ID */
    private Long userId;

    /** 是否公开：0-私有，1-公开 */
    private Integer isPublic;

    /** 照片数量（冗余字段，提高查询效率） */
    private Integer photoCount;

    /** 逻辑删除标志：0-正常，1-已删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
