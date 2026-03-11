package com.example.airecruit.application.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalyzedEvent {

    private Long applicationId;
    private Long userId;
    private Long jobPostingId;
    private String jobTitle;
    private Integer compatibilityScore;
    private String summary;
    private LocalDateTime analyzedAt;
}
