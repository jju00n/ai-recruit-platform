package com.example.airecruit.application.dto;

import java.util.List;

public class AiJobReviewDto {

    private AiJobReviewDto() {}

    public record Response(
            int compatibilityScore,
            String summary,
            List<String> strengths,
            List<String> weaknesses,
            List<String> tips
    ) {}
}
