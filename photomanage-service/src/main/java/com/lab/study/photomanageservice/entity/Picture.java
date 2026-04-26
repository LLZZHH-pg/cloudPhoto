package com.lab.study.photomanageservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "picture_info", autoResultMap = true)
public class Picture {
    @TableId(value = "pictureid", type = IdType.AUTO)
    private Integer pictureid;

    private Integer userid;

    private String fileHash;

    private String fileUrl;

    private String fileName;

    private Integer fileSize;

    private LocalDateTime shotTime;

    private LocalDateTime deleteTime;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String fileExif;
}