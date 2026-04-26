package com.lab.study.userservice.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 * 用于生成和解析 Token
 */
public class JwtUtil {

    /**
     * 生成 JWT Token
     *
     * @param subject 主题（通常是用户名或ID）
     * @param ttlMillis 有效期（毫秒）
     * @param claims 自定义声明（如 userId, role 等）
     * @return Token 字符串
     */
    public static String createJWT(String subject, Long ttlMillis, Map<String, Object> claims) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + ttlMillis;

        // 生成密钥 (生产环境建议将 secret 放在配置文件中)
        SecretKey key = Keys.hmacShaKeyFor("cloud-photo-secret-key-2024".getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(subject)           // 主题
                .setIssuedAt(new Date(nowMillis)) // 签发时间
                .setExpiration(new Date(expMillis)) // 过期时间
                .addClaims(claims)             // 自定义声明
                .signWith(key)                 // 签名
                .compact();
    }

    /**
     * 解析 JWT Token
     *
     * @param token Token 字符串
     * @return Claims 包含用户信息的对象
     */
    public static Claims parseJWT(String token) {
        SecretKey key = Keys.hmacShaKeyFor("cloud-photo-secret-key-2024".getBytes(StandardCharsets.UTF_8));

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证 Token 是否有效（未过期）
     */
    public static boolean verifyToken(String token) {
        try {
            parseJWT(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}