package com.example.airecruit.notification.kafka;

import com.example.airecruit.notification.client.UserServiceClient;
import com.example.airecruit.notification.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final EmailNotificationService emailNotificationService;
    private final UserServiceClient userServiceClient;

    @KafkaListener(topics = "application.submitted", groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleApplicationSubmitted(ApplicationSubmittedEvent event) {
        log.info("application.submitted 이벤트 수신 — applicationId={}, userId={}", event.getApplicationId(), event.getUserId());
        String email = userServiceClient.getUserEmail(event.getUserId());
        if (email == null) {
            log.warn("이메일 조회 실패 — userId={}, 알림 스킵", event.getUserId());
            return;
        }
        try {
            emailNotificationService.sendApplicationSubmittedEmail(email, event.getJobTitle(), event.getApplicationId());
        } catch (Exception e) {
            log.error("지원 완료 이메일 발송 실패 — email={}, error={}", email, e.getMessage());
        }
    }

    @KafkaListener(topics = "resume.analyzed", groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleResumeAnalyzed(ResumeAnalyzedEvent event) {
        log.info("resume.analyzed 이벤트 수신 — applicationId={}, userId={}, score={}", event.getApplicationId(), event.getUserId(), event.getCompatibilityScore());
        String email = userServiceClient.getUserEmail(event.getUserId());
        if (email == null) {
            log.warn("이메일 조회 실패 — userId={}, 알림 스킵", event.getUserId());
            return;
        }
        try {
            emailNotificationService.sendResumeAnalyzedEmail(
                    email,
                    event.getJobTitle(),
                    event.getCompatibilityScore() != null ? event.getCompatibilityScore() : 0,
                    event.getSummary() != null ? event.getSummary() : "분석 결과를 불러올 수 없습니다."
            );
        } catch (Exception e) {
            log.error("AI 분석 완료 이메일 발송 실패 — email={}, error={}", email, e.getMessage());
        }
    }

    @KafkaListener(topics = "job.created", groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleJobCreated(JobCreatedEvent event) {
        log.info("job.created 이벤트 수신 — jobPostingId={}, title={}, company={}", event.getJobPostingId(), event.getTitle(), event.getCompanyName());
        // TODO Phase 7: 관심 키워드 구독자에게 새 공고 알림
    }
}
