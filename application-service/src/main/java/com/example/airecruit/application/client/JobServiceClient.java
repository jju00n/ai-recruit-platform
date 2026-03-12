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
    public record JobDetail(Long id, String title, String companyName, String description,
                            String responsibilities, String requirements, String preferredQualifications) {}

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

    /**
     * 이력서 임베딩 벡터로 job-service kNN 검색.
     * job-service에 OPENAI_API_KEY 미설정이거나 벡터가 null이면 빈 리스트 반환.
     */
    public List<JobSummary> knnSearch(float[] vector) {
        if (vector == null || vector.length == 0) return List.of();

        try {
            List<Float> vectorList = new ArrayList<>(vector.length);
            for (float v : vector) vectorList.add(v);

            String responseBody = jobServiceRestClient.post()
                    .uri("/api/v1/jobs/search/vector?k=10")
                    .body(vectorList)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null) return List.of();

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.path("data");

            List<JobSummary> jobs = new ArrayList<>();
            if (data.isArray()) {
                for (JsonNode node : data) {
                    jobs.add(new JobSummary(
                            node.path("id").asLong(),
                            node.path("title").asText(""),
                            node.path("companyName").asText(""),
                            node.path("description").asText("")
                    ));
                }
            }
            return jobs;
        } catch (Exception e) {
            log.warn("[JobServiceClient] kNN 검색 실패 (fallback): {}", e.getMessage());
            return List.of();
        }
    }

    public JobDetail getJobById(Long jobId) {
        try {
            String responseBody = jobServiceRestClient.get()
                    .uri("/api/v1/jobs/{id}", jobId)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null) {
                throw new BizException(Status.JOB_POSTING_NOT_FOUND);
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.path("data");

            return new JobDetail(
                    data.path("id").asLong(),
                    data.path("title").asText(""),
                    data.path("company").path("name").asText(""),
                    data.path("description").asText(""),
                    data.path("responsibilities").asText(""),
                    data.path("requirements").asText(""),
                    data.path("preferredQualifications").asText("")
            );
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("[JobServiceClient] 채용공고 상세 조회 실패 - jobId={}: {}", jobId, e.getMessage(), e);
            throw new BizException(Status.JOB_SERVICE_ERROR);
        }
    }
}
