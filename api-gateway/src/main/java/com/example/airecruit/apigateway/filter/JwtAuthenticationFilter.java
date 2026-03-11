package com.example.airecruit.apigateway.filter;

import com.example.airecruit.apigateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // JWT 검증 없이 통과시킬 경로
    private static final List<String> WHITE_LIST = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/signup",
            "/api/v1/auth/reissue",
            "/api/v1/auth/oauth/kakao",
            "/api/v1/auth/oauth/apple",
            "/api/v1/users/signup",
            "/api/v1/users/email",
            "/api/v1/users/login/kakao",
            "/api/v1/users/login/apple",
            "/api/v1/jobs"
    );

    private final JwtProperties jwtProperties;
    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest());
        if (token == null) {
            return sendUnauthorized(exchange, "Missing token");
        }

        Claims claims;
        try {
            claims = parseToken(token);
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return sendUnauthorized(exchange, "Invalid token");
        }

        String userId = claims.getSubject();
        Object idxObj = claims.get("idx");
        String userIdx = idxObj != null ? String.valueOf(((Number) idxObj).longValue()) : userId;

        // Redis 블랙리스트 확인 (로그아웃된 토큰 차단)
        return redisTemplate.hasKey("blacklist:" + token)
                .flatMap(isBlacklisted -> {
                    if (Boolean.TRUE.equals(isBlacklisted)) {
                        log.warn("Blacklisted token used by userId={}", userId);
                        return sendUnauthorized(exchange, "Token has been invalidated");
                    }

                    // 검증 통과 → idx(PK)를 X-User-Id 헤더로, userId(email)를 X-User-Email 헤더로 전달
                    ServerWebExchange mutated = exchange.mutate()
                            .request(r -> r.header("X-User-Id", userIdx).header("X-User-Email", userId))
                            .build();
                    return chain.filter(mutated);
                });
    }

    private boolean isWhitelisted(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private Claims parseToken(String token) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getKey());
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Mono<Void> sendUnauthorized(ServerWebExchange exchange, String message) {
        log.warn("Unauthorized - path={}, reason={}", exchange.getRequest().getPath(), message);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"status\":\"UNAUTHORIZED\",\"message\":\"%s\"}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // 가장 먼저 실행
    }
}
