package com.example.airecruit.user.service;

import com.example.airecruit.user.domain.SocialAccount;
import com.example.airecruit.user.domain.User;
import com.example.airecruit.user.dto.LoginDto;
import com.example.airecruit.user.dto.KakaoUserInfoResponse;
import com.example.airecruit.user.dto.SignUpDto;
import com.example.airecruit.user.dto.UserDto;
import com.example.airecruit.user.jwt.JwtTokenProvider;
import com.example.airecruit.user.repository.SocialAccountRepository;
import com.example.airecruit.user.repository.UserRepository;
import com.example.airecruit.common.dto.ResultData;
import com.example.airecruit.common.dto.Status;
import com.example.airecruit.common.exception.LoginFailException;
import com.example.airecruit.user.util.AppleJwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AppleJwtUtils appleJwtUtils;
    private final SocialAccountRepository socialAccountRepository;
    private final KakaoService kakaoService;

    private final long refreshTokenValidityInMilliseconds = 14 * 24 * 60 * 60 * 1000L;

    @Value("${apple.app.bundle-id}")
    private String bundleId;

    @Value("${apple.app.client-id-expo:}")
    private String clientIdExpo;

    @Value("${apple.app.client-id-web:}")
    private String clientIdWeb;

    @Transactional
    public ResponseEntity<ResultData<?>> signUp(SignUpDto.SignUpReq dto) {
        try {
            if (userRepository.findByUserId(dto.getUserId()).isPresent()) {
                return ResponseEntity.badRequest().body(ResultData.of(Status.EXIST_ID));
            }
            User user = User.builder()
                    .userNm(dto.getUserNm())
                    .userId(dto.getUserId())
                    .userPw(passwordEncoder.encode(dto.getUserPw()))
                    .phone(dto.getPhone())
                    .role("ROLE_USER")
                    .build();
            userRepository.save(user);
            log.info("회원가입 완료: {}", dto.getUserId());
            return ResponseEntity.ok(ResultData.of(Status.SUCCESS));
        } catch (Exception e) {
            log.error("회원가입 실패", e);
            return ResponseEntity.internalServerError().body(ResultData.of(Status.ERROR_OCCURRED));
        }
    }

    public void sendEmailVerification(String email) {
        if (userRepository.findByUserId(email).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        emailService.sendVerificationCode(email);
    }

    public boolean verifyEmail(String email, String code) {
        return emailService.verifyCode(email, code);
    }

    @Transactional(readOnly = true)
    public UserDto getUserInfo(Long idx) {
        User user = userRepository.findById(idx)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다. ID: " + idx));
        return UserDto.of(user);
    }

    @Transactional(readOnly = true)
    public String getUserEmail(Long idx) {
        User user = userRepository.findById(idx)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다. ID: " + idx));
        return user.getUserId();
    }

    @Transactional
    public LoginDto.Response loginWithApple(LoginDto.AppleLoginRequestDto dto) {
        Claims claims = appleJwtUtils.getClaims(dto.getIdentityToken());

        List<String> allowedAudiences = List.of(bundleId, clientIdExpo, clientIdWeb);
        if (allowedAudiences.stream().noneMatch(claims.getAudience()::equals)) {
            throw new IllegalArgumentException("Audience 값이 일치하지 않습니다.");
        }

        String appleSocialId = claims.getSubject();
        String email = claims.get("email", String.class);

        if (email == null) {
            SocialAccount socialAccount = socialAccountRepository.findByProviderAndSocialId("APPLE", appleSocialId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
            return issueTokensAndSaveToRedis(socialAccount.getUser());
        }

        Optional<SocialAccount> socialAccountOpt = socialAccountRepository.findByProviderAndSocialId("APPLE", appleSocialId);
        if (socialAccountOpt.isPresent()) {
            return issueTokensAndSaveToRedis(socialAccountOpt.get().getUser());
        }

        Optional<User> userOpt = userRepository.findByUserId(email);
        if (userOpt.isPresent()) {
            User existingUser = userOpt.get();
            socialAccountRepository.save(SocialAccount.builder().user(existingUser).provider("APPLE").socialId(appleSocialId).build());
            return issueTokensAndSaveToRedis(existingUser);
        }

        User newUser = User.builder().userId(email).userNm(dto.getUserNm()).role("ROLE_USER").build();
        userRepository.save(newUser);
        socialAccountRepository.save(SocialAccount.builder().user(newUser).provider("APPLE").socialId(appleSocialId).build());
        return issueTokensAndSaveToRedis(newUser);
    }

    @Transactional
    public LoginDto.Response loginWithKakao(LoginDto.KakaoLoginRequestDto dto) {
        KakaoUserInfoResponse userInfo;
        if (dto.getAccessToken() != null) {
            log.info("네이티브 카카오 로그인 요청 (AccessToken)");
            userInfo = kakaoService.getUserInfoWithToken(dto.getAccessToken());
        } else if (dto.getCode() != null) {
            log.info("웹 카카오 로그인 요청 (Code)");
            userInfo = kakaoService.getUserInfoWithCode(dto.getCode(), dto.getCode_verifier());
        } else {
            throw new IllegalArgumentException("카카오 로그인 요청 정보가 올바르지 않습니다.");
        }

        String kakaoSocialId = userInfo.getId().toString();
        String email = userInfo.getKakaoAccount().getEmail();

        if (email == null) {
            SocialAccount socialAccount = socialAccountRepository.findByProviderAndSocialId("KAKAO", kakaoSocialId)
                    .orElseThrow(() -> new IllegalArgumentException("이메일 동의가 필요합니다."));
            return issueTokensAndSaveToRedis(socialAccount.getUser());
        }

        Optional<SocialAccount> socialAccountOpt = socialAccountRepository.findByProviderAndSocialId("KAKAO", kakaoSocialId);
        if (socialAccountOpt.isPresent()) {
            return issueTokensAndSaveToRedis(socialAccountOpt.get().getUser());
        }

        Optional<User> userOpt = userRepository.findByUserId(email);
        if (userOpt.isPresent()) {
            User existingUser = userOpt.get();
            socialAccountRepository.save(SocialAccount.builder().user(existingUser).provider("KAKAO").socialId(kakaoSocialId).build());
            return issueTokensAndSaveToRedis(existingUser);
        }

        String nickname = userInfo.getKakaoAccount().getProfile().getNickname();
        User newUser = User.builder().userId(email).userNm(nickname).role("ROLE_USER").build();
        userRepository.save(newUser);
        socialAccountRepository.save(SocialAccount.builder().user(newUser).provider("KAKAO").socialId(kakaoSocialId).build());
        return issueTokensAndSaveToRedis(newUser);
    }

    private LoginDto.Response issueTokensAndSaveToRedis(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getIdx(), user.getUserId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
        redisTemplate.opsForValue().set("RT:" + user.getUserId(), refreshToken, refreshTokenValidityInMilliseconds, TimeUnit.MILLISECONDS);
        return new LoginDto.Response(accessToken, refreshToken);
    }
}
