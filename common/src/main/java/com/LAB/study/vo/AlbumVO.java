package com.LAB.study.vo;

import com.LAB.study.dto.PictureDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 影集详情响应
 */
@Data
public class AlbumVO {

    private Long id;
    private String name;
    private String description;
    private String coverUrl;
    private Integer userId;
    private Integer photoCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 影集内的照片ID列表（详情接口返回） */
    private List<Long> photoIds;
    /** 关联的图片详细数据 */
    private List<PictureDTO> photos;
}
