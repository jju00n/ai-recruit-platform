package com.example.airecruit.application.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationEventProducer {

    private static final String TOPIC_APPLICATION_SUBMITTED = "application.submitted";
    private static final String TOPIC_RESUME_ANALYZED = "resume.analyzed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishApplicationSubmitted(ApplicationSubmittedEvent event) {
        kafkaTemplate.send(TOPIC_APPLICATION_SUBMITTED, event.getApplicationId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("[Kafka] application.submitted 발행 실패 - applicationId={}, error={}",
                                event.getApplicationId(), ex.getMessage());
                    } else {
                        log.info("[Kafka] application.submitted 발행 완료 - applicationId={}, offset={}",
                                event.getApplicationId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void publishResumeAnalyzed(ResumeAnalyzedEvent event) {
        kafkaTemplate.send(TOPIC_RESUME_ANALYZED, event.getApplicationId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("[Kafka] resume.analyzed 발행 실패 - applicationId={}, error={}",
                                event.getApplicationId(), ex.getMessage());
                    } else {
                        log.info("[Kafka] resume.analyzed 발행 완료 - applicationId={}, offset={}",
                                event.getApplicationId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
