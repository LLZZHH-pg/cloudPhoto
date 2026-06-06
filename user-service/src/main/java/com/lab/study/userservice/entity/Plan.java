package com.lab.study.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("plan_info")
public class Plan {
    @TableId(value = "planid", type = IdType.AUTO)
    private Integer planid;
    private String name;
    private Long storage;
    private Integer recycle;
    private BigDecimal price;
    private String statues;
}
