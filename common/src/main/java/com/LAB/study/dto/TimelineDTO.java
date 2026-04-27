package com.LAB.study.dto;
import lombok.Data;
import java.util.List;

@Data
public class TimelineDTO {
    private String date;
    private List<PictureDTO> pictures;
}
