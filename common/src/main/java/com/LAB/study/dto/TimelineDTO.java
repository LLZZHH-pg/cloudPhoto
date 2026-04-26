package com.LAB.study.dto;
import lombok.Data;
import java.util.List;

@Data
public class TimelineDTO {
    private String date; // 按照日期分组，如 "2023-10-25"
    private List<PictureDTO> pictures;
}
