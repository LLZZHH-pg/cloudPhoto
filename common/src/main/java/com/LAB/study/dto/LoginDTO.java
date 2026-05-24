package com.LAB.study.dto;
import lombok.Data;

@Data
public class LoginDTO {
    private String acc;   // 邮箱 / 手机号 / 用户名
    private String pas;
}
