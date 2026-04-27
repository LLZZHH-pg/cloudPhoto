package com.lab.study.gatewayservice.filter;


import com.LAB.study.dto.JwtUserDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String REDIS_KEY_PREFIX = "auth:token:";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/user/auth/register",
            "/api/user/auth/login"
    );

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.info("Gateway JWT secret key initialized.");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = resolveToken(exchange);
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, HttpStatus.UNAUTHORIZED, "未提供Token");
        }

        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired", e);
            return unauthorized(exchange, HttpStatus.UNAUTHORIZED, "Token已过期");
        } catch (MalformedJwtException | SecurityException e) {
            log.warn("Invalid JWT token", e);
            return unauthorized(exchange, HttpStatus.FORBIDDEN, "无效的Token");
        } catch (Exception e) {
            log.error("JWT token validation failed", e);
            return unauthorized(exchange, HttpStatus.FORBIDDEN, "Token验证失败");
        }

        String jti = claims.getId();
        if (!StringUtils.hasText(jti)) {
            return unauthorized(exchange, HttpStatus.FORBIDDEN, "Token缺少jti");
        }

        String redisKey = REDIS_KEY_PREFIX + jti;
        String tokenInRedis = redisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(tokenInRedis) || !token.equals(tokenInRedis)) {
            return unauthorized(exchange, HttpStatus.UNAUTHORIZED, "Token无效或已过期");
        }

        Object userClaim = claims.get("user");
        if (userClaim == null) {
            return unauthorized(exchange, HttpStatus.FORBIDDEN, "Token缺少用户信息");
        }

        JwtUserDTO jwtUser = objectMapper.convertValue(userClaim, JwtUserDTO.class);

        try {
            String userJson = objectMapper.writeValueAsString(jwtUser);
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(builder -> builder.header("X-User-Info", userJson))
                    .build();
            return chain.filter(mutatedExchange);
        } catch (JsonProcessingException e) {
            log.error("Serialize JwtUserDTO failed", e);
            return unauthorized(exchange, HttpStatus.FORBIDDEN, "Token解析失败");
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private String resolveToken(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String bearerToken = headers.getFirst(AUTH_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        for (String pattern : PUBLIC_PATHS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange,
                                    HttpStatus status,
                                    String message) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");

        String path = exchange.getRequest().getURI().getPath();
        String body = String.format(
                "{\"code\":%d,\"msg\":\"%s\",\"path\":\"%s\"}",
                status.value(),
                message,
                path
        );
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
