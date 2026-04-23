package com.lab.study.albummanageservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 影集-照片关联表
 */
@Data
@TableName("album_photo")
public class AlbumPhoto {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 影集ID */
    private Long albumId;

    /** 照片ID（关联 photomanage-service） */
    private Long photoId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
