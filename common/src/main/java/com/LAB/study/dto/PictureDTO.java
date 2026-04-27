package com.LAB.study.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PictureDTO {
    private Long pictureid;
    private String fileName;
    private LocalDateTime shotTime;
    private String fileUrl;
    private String previewUrl;
    private String fileExif;
}
