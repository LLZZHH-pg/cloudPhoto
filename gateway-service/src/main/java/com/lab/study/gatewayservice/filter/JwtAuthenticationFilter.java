package com.lab.study.gatewayservice.filter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
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
import java.util.Date;
@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    @Value("${jwt.secret:defaultSecret}")
    private String secret;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        log.debug("Processing request for path: {}", path);
        if (isPublicEndpoint(path)) {
            log.info("Accessing public endpoint: {}", path);
            return chain.filter(exchange);
        }
        String token = getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            log.warn("Missing token in Authorization header for path: {}", path);
            return handleUnauthorizedResponse(exchange, "No token provided");
        }
        return Mono.defer(() -> {
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
                if (claims.getExpiration().before(new Date())) {
                    log.warn("Token has expired for path: {} and user: {}", path, claims.getSubject());
                    return handleUnauthorizedResponse(exchange, "Token has expired");
                }
                String userId = (String) claims.get("userId");
                String username = claims.getSubject();
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId != null ? userId : "")
                        .header("X-Username", username != null ? username : "")
                        .build();
                log.info("JWT validation successful for user: '{}' (ID: {}) on path: {}", username, userId, path);
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } catch (MalformedJwtException e) {
                log.warn("Malformed JWT token for path: {} - {}", path, e.getMessage());
                return handleUnauthorizedResponse(exchange, "Malformed token");
            } catch (ExpiredJwtException e) {
                log.warn("Expired JWT token for path: {} - {}", path, e.getMessage());
                return handleUnauthorizedResponse(exchange, "Token has expired");
            } catch (UnsupportedJwtException e) {
                log.warn("Unsupported JWT token for path: {} - {}", path, e.getMessage());
                return handleUnauthorizedResponse(exchange, "Unsupported token format");
            } catch (SignatureException e) {
                log.warn("Invalid signature for JWT token on path: {} - {}", path, e.getMessage());
                return handleUnauthorizedResponse(exchange, "Invalid token signature");
            } catch (IllegalArgumentException e) {
                log.warn("Illegal argument when parsing JWT for path: {} - {}", path, e.getMessage());
                return handleUnauthorizedResponse(exchange, "Invalid token format");
            } catch (Exception e) {
                log.error("Unexpected error during JWT validation for path: {} - Exception type: {}, Message: {}",
                        path, e.getClass().getSimpleName(), e.getMessage(), e);
                return handleUnauthorizedResponse(exchange, "Invalid token");
            }
        }).onErrorMap(Exception.class, ex -> {
            log.error("Critical error in JWT filter for path: {} - {}", path, ex.getMessage(), ex);
            return new RuntimeException("JWT filter internal error", ex);
        });
    }
    private String getTokenFromRequest(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/auth/login") ||
                path.startsWith("/auth/register") ||
                path.startsWith("/actuator") ||
                path.startsWith("/error") ||
                path.equals("/api/test");
    }
    private Mono<Void> handleUnauthorizedResponse(ServerWebExchange exchange, String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        String body = String.format("{\"code\": 401, \"message\": \"Unauthorized: %s\", \"path\": \"%s\"}",
                reason, exchange.getRequest().getURI().getPath());
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        log.debug("Sending 401 response for path: {}, reason: {}",
                exchange.getRequest().getURI().getPath(), reason);
        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(bytes))
        );
    }
    @Override
    public int getOrder() {
        return -1;
    }
}