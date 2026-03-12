package com.example.airecruit.application.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * OpenAI text-embedding-3-small 모델로 텍스트 임베딩 생성.
 * OPENAI_API_KEY 미설정 시 null 반환 → 기존 키워드 필터링 fallback.
 */
@Service
@Slf4j
public class EmbeddingService {

    private static final String MODEL = "text-embedding-3-small";
    private static final int MAX_CHARS = 8000;

    @Value("${openai.api.key:}")
    private String apiKey;

    private RestClient openaiClient;

    @PostConstruct
    public void init() {
        openaiClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * @return 1536차원 임베딩 벡터, API key 미설정 또는 실패 시 null
     */
    public float[] embed(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        if (text == null || text.isBlank()) {
            return null;
        }

        String input = text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;

        try {
            Map<String, Object> body = Map.of("model", MODEL, "input", input);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = openaiClient.post()
                    .uri("/v1/embeddings")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return null;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            if (data == null || data.isEmpty()) return null;

            @SuppressWarnings("unchecked")
            List<Number> embedding = (List<Number>) data.get(0).get("embedding");
            if (embedding == null) return null;

            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i).floatValue();
            }
            return vector;

        } catch (Exception e) {
            log.warn("[EmbeddingService] 임베딩 생성 실패 (무시): {}", e.getMessage());
            return null;
        }
    }
}
