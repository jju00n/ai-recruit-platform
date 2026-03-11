package com.example.airecruit.application.kafka;

import com.example.airecruit.application.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationEventConsumer {

    private final AiAnalysisService aiAnalysisService;

    @KafkaListener(topics = "application.submitted", groupId = "application-service-group")
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        log.info("[Kafka] application.submitted 수신 - applicationId={}", event.getApplicationId());
        try {
            aiAnalysisService.analyze(event);
        } catch (Exception e) {
            log.error("[Kafka] AI 분석 처리 실패 - applicationId={}, error={}",
                    event.getApplicationId(), e.getMessage(), e);
        }
    }
}
