package com.LAB.study.request;

import lombok.Data;

@Data
public class UserStatusRequest {
    private Integer userId;
    private String status; // 限定 'enable', 'disable', 'auth'
}
