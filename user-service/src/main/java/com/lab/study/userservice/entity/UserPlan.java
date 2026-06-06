package com.lab.study.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_plan")
public class UserPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userid;
    private Integer planid;
    private LocalDateTime createdAt;
}
