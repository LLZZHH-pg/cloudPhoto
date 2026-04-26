package com.lab.study.gatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret:defaultSecret}")
    private String secret;

    // JWT token在请求头中的键名
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 获取请求路径，对某些公共接口跳过认证
        String path = request.getURI().getPath();
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        String token = getTokenFromRequest(request);

        if (!StringUtils.hasText(token)) {
            log.warn("Token is missing in request header");
            return handleUnauthorizedResponse(exchange);
        }

        try {
            // 解析JWT令牌
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(secret.getBytes())
                    .build()
                    .parseClaimsJws(token);

            // 验证令牌是否过期
            if (claims.getBody().getExpiration().before(new java.util.Date())) {
                log.warn("Token has expired");
                return handleUnauthorizedResponse(exchange);
            }

            // 将用户信息添加到请求头中传递给下游服务
            String userId = (String) claims.getBody().get("userId");
            String username = claims.getBody().getSubject(); // 直接赋值即可
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-Username", username)
                    .build();

            log.info("JWT validation successful for user: {}", username);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return handleUnauthorizedResponse(exchange);
        }
    }

    private String getTokenFromRequest(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private boolean isPublicEndpoint(String path) {
        // 定义不需要认证的公共端点
        return path.startsWith("/auth/login") ||
                path.startsWith("/auth/register") ||
                path.startsWith("/actuator") ||
                path.startsWith("/error");
    }

    private Mono<Void> handleUnauthorizedResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);

        String body = "{\"code\": 401, \"message\": \"Unauthorized: Invalid or missing token\"}";
        byte[] bytes = body.getBytes();
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(bytes))
        );
    }

    @Override
    public int getOrder() {
        // 设置过滤器顺序，值越小优先级越高
        return -1;
    }
}