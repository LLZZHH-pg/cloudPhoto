package com.LAB.study.dto;

import lombok.Data;

@Data
public class UserInfoDTO {
    private Integer userId;
    private String nam;
    private String tel;
    private String eml;
    private Long totalstorage;
    private Long usedstorage;
}