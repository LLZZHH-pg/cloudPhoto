package com.LAB.study.vo;

import lombok.Data;
import java.util.List;

@Data
public class TimelineVO {
    private String date;
    private List<PictureVO> pictures;
}
