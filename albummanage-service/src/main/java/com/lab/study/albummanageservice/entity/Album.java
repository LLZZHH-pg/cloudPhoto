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
    private Integer userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
