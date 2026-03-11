package com.example.airecruit.job.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobEventProducer {

    private static final String TOPIC_JOB_CREATED = "job.created";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishJobCreated(JobCreatedEvent event) {
        kafkaTemplate.send(TOPIC_JOB_CREATED, event.getJobPostingId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("[Kafka] job.created 발행 실패 - jobPostingId={}, error={}",
                                event.getJobPostingId(), ex.getMessage());
                    } else {
                        log.info("[Kafka] job.created 발행 완료 - jobPostingId={}, offset={}",
                                event.getJobPostingId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
