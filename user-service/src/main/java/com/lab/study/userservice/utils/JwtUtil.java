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

    public static String createJWT(SecretKey key, String subject, Long ttlMillis, Map<String, Object> claims) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + ttlMillis;

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(new Date(expMillis))
                .addClaims(claims)
                .signWith(key)                     // 使用传入的 key
                .compact();
    }

    public static Claims parseJWT(String token, SecretKey key) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static boolean verifyToken(String token, SecretKey key) {
        try {
            parseJWT(token, key);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}