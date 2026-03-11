package com.example.airecruit.job.kafka;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class JobCreatedEvent {

    private Long jobPostingId;
    private Long companyId;
    private String companyName;
    private String title;
    private String jobCategory;
    private String location;
    private LocalDateTime createdAt;
}
