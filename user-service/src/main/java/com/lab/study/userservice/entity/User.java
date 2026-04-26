package com.lab.study.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

@Data
@TableName("user_info") // 对应数据库表名
public class User {

    @TableId(value = "userid", type = IdType.AUTO)
    private Integer userId; // 对应 userid

    private String nam; // 对应 nam (用户名)
    @JsonProperty(access = Access.WRITE_ONLY)  // 只反序列化（接受输入），序列化时忽略（不输出）
    private String pas;
    private String tel; // 对应 tel
    private String eml; // 对应 eml

    private Long usedstorage; // 对应 usedstorage
    private Long totalstorage; // 对应 totalstorage
}