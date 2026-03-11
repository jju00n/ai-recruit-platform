package com.example.airecruit.auth.service;

import com.example.airecruit.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    public void logout(String accessToken) {
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        // 1. accessToken 블랙리스트 등록
        Long remainingTime = jwtTokenProvider.getExpiration(accessToken);
        if (remainingTime > 0) {
            redisTemplate.opsForValue().set("blacklist:" + accessToken, "logout", remainingTime, TimeUnit.MILLISECONDS);
        }

        // 2. Redis에서 refreshToken 삭제
        String userId = jwtTokenProvider.getUserIdFromToken(accessToken);
        redisTemplate.delete("RT:" + userId);
    }
}
