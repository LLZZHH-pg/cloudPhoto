package com.lab.study.photomanageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 上传结果DTO，包含照片ID和AI推荐的分类名称
 */
@Data
@AllArgsConstructor
public class UploadResultDTO {
    private Long photoId;
    private String category;
}
