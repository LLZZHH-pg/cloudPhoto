package com.LAB.study.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PictureVO {
    private Long pictureid;
    private String fileName;
    private LocalDateTime shotTime;
    private LocalDateTime deleteTime;
    private String previewUrl;
    private String fileExif;
}
