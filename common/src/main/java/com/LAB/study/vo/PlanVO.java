package com.LAB.study.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PlanVO {
    private Integer planid;
    private String name;
    private Long storage;
    private Integer recycle;
    private BigDecimal price;
    private String statues;
}
