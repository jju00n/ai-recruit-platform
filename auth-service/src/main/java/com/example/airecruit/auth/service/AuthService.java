package com.example.airecruit.auth.service;

import com.example.airecruit.auth.domain.User;
import com.example.airecruit.auth.dto.LoginDto;
import com.example.airecruit.auth.jwt.JwtTokenProvider;
import com.example.airecruit.auth.repository.UserRepository;
import com.example.airecruit.common.exception.LoginFailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    private final long refreshTokenValidityInMilliseconds = 14 * 24 * 60 * 60 * 1000L;

    @Transactional
    public LoginDto.Response login(LoginDto.Request request) {
        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 아이디입니다."));

        if (!passwordEncoder.matches(request.getUserPw(), user.getUserPw())) {
            throw new LoginFailException();
        }

        return issueTokensAndSaveToRedis(user.getIdx(), user.getUserId(), user.getRole());
    }

    @Transactional
    public LoginDto.Response reissue(LoginDto.ReissueRequestDto dto) {
        if (!jwtTokenProvider.validateToken(dto.getRefreshToken())) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(dto.getRefreshToken());
        String savedRefreshToken = (String) redisTemplate.opsForValue().get("RT:" + userId);

        if (ObjectUtils.isEmpty(savedRefreshToken)) {
            throw new IllegalArgumentException("로그아웃된 사용자입니다.");
        }
        if (!savedRefreshToken.equals(dto.getRefreshToken())) {
            throw new IllegalArgumentException("리프레시 토큰 정보가 일치하지 않습니다.");
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다: " + userId));

        return issueTokensAndSaveToRedis(user.getIdx(), user.getUserId(), user.getRole());
    }

    public LoginDto.Response issueTokensAndSaveToRedis(Long idx, String userId, String role) {
        String accessToken = jwtTokenProvider.createAccessToken(idx, userId, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);
        redisTemplate.opsForValue().set("RT:" + userId, refreshToken, refreshTokenValidityInMilliseconds, TimeUnit.MILLISECONDS);
        return new LoginDto.Response(accessToken, refreshToken);
    }
}
