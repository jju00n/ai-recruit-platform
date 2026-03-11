package com.example.airecruit.application.client;

import com.example.airecruit.common.dto.Status;
import com.example.airecruit.common.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobServiceClient {

    private final RestClient jobServiceRestClient;
    private final ObjectMapper objectMapper;

    public record JobSummary(Long id, String title, String companyName, String description) {}

    public List<JobSummary> getJobs() {
        try {
            String responseBody = jobServiceRestClient.get()
                    .uri("/api/v1/jobs?page=0&size=20")
                    .retrieve()
                    .body(String.class);

            if (responseBody == null) {
                throw new BizException(Status.JOB_SERVICE_ERROR);
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("data").path("content");

            List<JobSummary> jobs = new ArrayList<>();
            if (content.isArray()) {
                for (JsonNode node : content) {
                    Long id = node.path("id").asLong();
                    String title = node.path("title").asText("");
                    String companyName = node.path("company").path("name").asText("");
                    String description = node.path("description").asText("");
                    jobs.add(new JobSummary(id, title, companyName, description));
                }
            }
            return jobs;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("[JobServiceClient] 채용공고 조회 실패: {}", e.getMessage(), e);
            throw new BizException(Status.JOB_SERVICE_ERROR);
        }
    }
}
