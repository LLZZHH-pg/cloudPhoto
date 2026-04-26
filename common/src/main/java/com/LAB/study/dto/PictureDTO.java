package com.LAB.study.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PictureDTO {
    private Integer pictureid;
    private String fileName;
    private LocalDateTime shotTime;
    private String fileUrl; // 原始链接或基础链接
    private String previewUrl; // 动态生成的七牛云预览/缩略图链接
    private String fileExif;
}
