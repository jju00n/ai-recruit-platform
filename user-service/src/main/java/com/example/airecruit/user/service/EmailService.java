package com.example.airecruit.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String EMAIL_VERIFY_PREFIX = "email:verify:";
    private static final int CODE_LENGTH = 6;
    private static final long CODE_EXPIRE_MINUTES = 5;

    public void sendVerificationCode(String email) {
        String code = generateVerificationCode();
        String key = EMAIL_VERIFY_PREFIX + email;
        stringRedisTemplate.opsForValue().set(key, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[AI Recruit] 이메일 인증 코드");
        message.setText(
                "안녕하세요.\n\n" +
                "회원가입을 위한 인증 코드입니다.\n\n" +
                "인증 코드: " + code + "\n\n" +
                "이 코드는 " + CODE_EXPIRE_MINUTES + "분 동안 유효합니다.\n\n" +
                "감사합니다."
        );
        mailSender.send(message);
        log.info("이메일 인증 코드 전송 완료: {}", email);
    }

    public boolean verifyCode(String email, String code) {
        String key = EMAIL_VERIFY_PREFIX + email;
        String savedCode = stringRedisTemplate.opsForValue().get(key);
        if (savedCode != null && savedCode.equals(code)) {
            stringRedisTemplate.delete(key);
            log.info("이메일 인증 성공: {}", email);
            return true;
        }
        log.warn("이메일 인증 실패: {}", email);
        return false;
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}
