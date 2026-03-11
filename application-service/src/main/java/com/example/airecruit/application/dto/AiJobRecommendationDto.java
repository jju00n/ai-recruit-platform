package com.example.airecruit.application.dto;

public class AiJobRecommendationDto {

    private AiJobRecommendationDto() {}

    public record Response(
            Long jobPostingId,
            String jobTitle,
            String companyName,
            int matchScore,
            String matchReason
    ) {}
}
