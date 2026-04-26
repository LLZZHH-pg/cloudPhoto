package com.lab.study.userservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Base64;
import javax.crypto.SecretKey;

@SpringBootApplication // 👈 保持纯净，不要加 exclude
public class UserServiceApplication {
    public static void main(String[] args) {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String secret = Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println("生成的密钥: " + secret);
        SpringApplication.run(UserServiceApplication.class, args);
    }
}