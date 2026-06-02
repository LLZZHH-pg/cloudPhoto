package com.LAB.study.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PictureDTO {
    private Long pictureid;
    private String previewUrl;
    private LocalDateTime shotTime;
    private String category;
}
