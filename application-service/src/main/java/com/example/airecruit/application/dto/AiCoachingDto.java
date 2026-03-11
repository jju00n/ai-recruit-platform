package com.example.airecruit.application.dto;

import java.util.List;

public class AiCoachingDto {

    private AiCoachingDto() {}

    public record Response(
            int overallScore,
            String summary,
            String structuralFeedback,
            String contentFeedback,
            List<Improvement> improvements
    ) {}

    public record Improvement(
            String section,
            String issue,
            String suggestion,
            String example
    ) {}
}
