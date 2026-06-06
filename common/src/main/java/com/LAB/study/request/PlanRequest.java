package com.LAB.study.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PlanRequest {
    private Integer planid; // 修改或删除时必传，新增时不传
    private String name;
    private Long storage;
    private Integer recycle;
    private BigDecimal price;
    private String statues; // 'enable', 'disable'
}