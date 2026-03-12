package com.example.airecruit.notification.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(@Value("${user-service.url}") String userServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }

    public String getUserEmail(Long userId) {
        try {
            JsonNode response = restClient.get()
                    .uri("/api/v1/users/internal/{idx}", userId)
                    .retrieve()
                    .body(JsonNode.class);
            if (response != null && response.has("data")) {
                JsonNode dataNode = response.get("data");
                if (!dataNode.isNull() && !dataNode.asText().isBlank()) {
                    return dataNode.asText();
                }
            }
            log.warn("userId={} 이메일 조회 응답 없음", userId);
            return null;
        } catch (Exception e) {
            log.error("userId={} 이메일 조회 실패: {}", userId, e.getMessage());
            return null;
        }
    }
}
