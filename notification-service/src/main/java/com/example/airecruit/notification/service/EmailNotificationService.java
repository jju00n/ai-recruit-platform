package com.example.airecruit.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    public void sendApplicationSubmittedEmail(String email, String jobTitle, Long applicationId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[AI Recruit] 지원이 완료되었습니다 — " + jobTitle);
        message.setText(
                "안녕하세요.\n\n" +
                "'" + jobTitle + "' 포지션에 지원해 주셔서 감사합니다.\n\n" +
                "지원 번호: " + applicationId + "\n\n" +
                "AI가 이력서를 분석 중입니다. 분석이 완료되면 별도로 안내드리겠습니다.\n\n" +
                "AI Recruit 드림"
        );
        mailSender.send(message);
        log.info("지원 완료 이메일 전송 — email={}, jobTitle={}, applicationId={}", email, jobTitle, applicationId);
    }

    public void sendResumeAnalyzedEmail(String email, String jobTitle, int compatibilityScore, String summary) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[AI Recruit] 이력서 AI 분석이 완료되었습니다 — " + jobTitle);
        message.setText(
                "안녕하세요.\n\n" +
                "'" + jobTitle + "' 포지션에 대한 AI 이력서 분석이 완료되었습니다.\n\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "적합도 점수: " + compatibilityScore + " / 100\n\n" +
                "AI 요약:\n" + summary + "\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "자세한 분석 결과는 앱에서 확인하실 수 있습니다.\n\n" +
                "AI Recruit 드림"
        );
        mailSender.send(message);
        log.info("AI 분석 완료 이메일 전송 — email={}, jobTitle={}, score={}", email, jobTitle, compatibilityScore);
    }
}
