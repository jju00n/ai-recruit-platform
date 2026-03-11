package com.example.airecruit.application.dto;

import com.example.airecruit.application.domain.AiFeedback;
import com.example.airecruit.application.domain.Application;
import com.example.airecruit.application.domain.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class ApplicationDto {

    private ApplicationDto() {}

    @Getter
    public static class SubmitReq {

        @NotNull(message = "채용공고 ID를 입력해주세요.")
        private Long jobPostingId;

        @NotNull(message = "이력서 ID를 입력해주세요.")
        private Long resumeId;
    }

    @Getter
    @Builder
    public static class Response {

        private Long id;
        private Long userId;
        private Long jobPostingId;
        private String jobTitle;
        private Long resumeId;
        private ApplicationStatus status;
        private LocalDateTime appliedAt;
        private LocalDateTime updatedAt;
        private Integer compatibilityScore;

        public static Response from(Application application, Integer compatibilityScore) {
            return Response.builder()
                    .id(application.getId())
                    .userId(application.getUserId())
                    .jobPostingId(application.getJobPostingId())
                    .jobTitle(application.getJobTitle())
                    .resumeId(application.getResumeId())
                    .status(application.getStatus())
                    .appliedAt(application.getAppliedAt())
                    .updatedAt(application.getUpdatedAt())
                    .compatibilityScore(compatibilityScore)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class DetailResponse {

        private Long id;
        private Long userId;
        private Long jobPostingId;
        private String jobTitle;
        private String jobDescription;
        private Long resumeId;
        private ApplicationStatus status;
        private LocalDateTime appliedAt;
        private LocalDateTime updatedAt;
        private AiFeedbackDto aiFeedback;

        @Getter
        @Builder
        public static class AiFeedbackDto {
            private Integer compatibilityScore;
            private String summary;
            private String strengths;
            private String weaknesses;
            private String recommendation;
            private LocalDateTime analyzedAt;

            public static AiFeedbackDto from(AiFeedback feedback) {
                return AiFeedbackDto.builder()
                        .compatibilityScore(feedback.getCompatibilityScore())
                        .summary(feedback.getSummary())
                        .strengths(feedback.getStrengths())
                        .weaknesses(feedback.getWeaknesses())
                        .recommendation(feedback.getRecommendation())
                        .analyzedAt(feedback.getAnalyzedAt())
                        .build();
            }
        }

        public static DetailResponse from(Application application, AiFeedback feedback) {
            return DetailResponse.builder()
                    .id(application.getId())
                    .userId(application.getUserId())
                    .jobPostingId(application.getJobPostingId())
                    .jobTitle(application.getJobTitle())
                    .jobDescription(application.getJobDescription())
                    .resumeId(application.getResumeId())
                    .status(application.getStatus())
                    .appliedAt(application.getAppliedAt())
                    .updatedAt(application.getUpdatedAt())
                    .aiFeedback(feedback != null ? AiFeedbackDto.from(feedback) : null)
                    .build();
        }
    }
}
