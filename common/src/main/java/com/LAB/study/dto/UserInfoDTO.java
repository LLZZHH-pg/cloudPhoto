package com.LAB.study.dto;

import lombok.Data;

@Data
public class UserInfoDTO {
    private Integer userId;
    private String nam;
    private String tel;
    private String eml;
    private Long usedstorage;
    private String statues;

    //额外查询
    private Long totalstorage;
    private Integer recycledays;

}