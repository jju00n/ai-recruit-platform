package com.example.airecruit.notification.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCreatedEvent {

    private Long jobPostingId;
    private Long companyId;
    private String companyName;
    private String title;
    private String jobCategory;
    private String location;
    private LocalDateTime createdAt;
}
