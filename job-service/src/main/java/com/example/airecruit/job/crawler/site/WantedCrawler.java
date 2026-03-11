package com.example.airecruit.job.crawler.site;

import com.example.airecruit.job.crawler.CrawledJobData;
import com.example.airecruit.job.domain.enums.JobSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WantedCrawler {

    private static final String LIST_URL =
            "https://www.wanted.co.kr/api/v4/jobs?country=kr&job_sort=job.latest_order&years=-1&locations=all&limit=100&offset=0";
    private static final String DETAIL_URL =
            "https://www.wanted.co.kr/api/v4/jobs/";
    private static final String JOB_URL_PREFIX =
            "https://www.wanted.co.kr/wd/";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public List<CrawledJobData> crawl() {
        List<CrawledJobData> result = new ArrayList<>();
        try {
            HttpHeaders headers = buildHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    LIST_URL, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode jobs = root.path("data");

            for (JsonNode job : jobs) {
                try {
                    CrawledJobData data = parseJob(job, headers);
                    if (data != null) result.add(data);
                } catch (Exception e) {
                    log.warn("[WantedCrawler] 공고 파싱 실패 - id={}, error={}",
                            job.path("id").asText(), e.getMessage());
                }
            }
            log.info("[WantedCrawler] 수집 완료 - {}건", result.size());
        } catch (Exception e) {
            log.error("[WantedCrawler] 크롤링 실패 - {}", e.getMessage());
        }
        return result;
    }

    private CrawledJobData parseJob(JsonNode job, HttpHeaders headers) throws Exception {
        long jobId = job.path("id").asLong();
        String sourceUrl = JOB_URL_PREFIX + jobId;

        // 상세 정보 조회
        ResponseEntity<String> detailRes = restTemplate.exchange(
                DETAIL_URL + jobId, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        JsonNode detail = objectMapper.readTree(detailRes.getBody()).path("job");

        String companyName = detail.path("company").path("name").asText("");
        String industry = detail.path("company").path("industry_name").asText("");
        String title = detail.path("position").asText("");
        String location = detail.path("address").path("location").asText("");

        JsonNode jobDetail = detail.path("detail");

        // 섹션별 분리
        String description        = jobDetail.path("intro").asText("").trim();
        String responsibilities   = jobDetail.path("main_tasks").asText("").trim();
        String requirements       = jobDetail.path("requirements").asText("").trim();
        String preferredPoints    = jobDetail.path("preferred_points").asText("").trim();
        String benefits           = jobDetail.path("benefits").asText("").trim();

        // 채용 전형: 배열 → 개행 구분 문자열
        StringBuilder hiringProcessBuilder = new StringBuilder();
        for (JsonNode step : detail.path("hiring_process")) {
            String stepText = step.asText("").trim();
            if (!stepText.isEmpty()) {
                if (hiringProcessBuilder.length() > 0) hiringProcessBuilder.append(" → ");
                hiringProcessBuilder.append(stepText);
            }
        }
        String hiringProcess = hiringProcessBuilder.toString();

        // 기술스택 (skill_tags: [{title: "Java"}, ...] 구조)
        StringBuilder skillBuilder = new StringBuilder();
        for (JsonNode tag : detail.path("skill_tags")) {
            String skillName = tag.path("title").asText(tag.path("parent_key_name").asText(""));
            if (!skillName.isEmpty()) {
                if (skillBuilder.length() > 0) skillBuilder.append(",");
                skillBuilder.append(skillName);
            }
        }

        // 마감일
        String dueTime = detail.path("due_time").asText("");
        String deadline = dueTime.length() >= 10 ? dueTime.substring(0, 10) : null;

        // 경력: requirements 텍스트에서 파싱
        String experienceLevel = requirements;

        // 직무 카테고리: position 텍스트로 추론
        String jobCategory = title;

        if (title.isEmpty() || companyName.isEmpty()) return null;

        return CrawledJobData.builder()
                .companyName(companyName)
                .companyIndustry(industry)
                .companyLocation(location)
                .title(title)
                .description(description.isEmpty() ? title : description)
                .responsibilities(responsibilities.isEmpty() ? null : responsibilities)
                .requirements(requirements.isEmpty() ? null : requirements)
                .preferredQualifications(preferredPoints.isEmpty() ? null : preferredPoints)
                .benefits(benefits.isEmpty() ? null : benefits)
                .hiringProcess(hiringProcess.isEmpty() ? null : hiringProcess)
                .location(location)
                .employmentType("정규직")
                .experienceLevel(experienceLevel)
                .jobCategory(jobCategory)
                .skills(skillBuilder.toString())
                .deadline(deadline)
                .sourceUrl(sourceUrl)
                .source(JobSource.WANTED)
                .build();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        headers.set("Referer", "https://www.wanted.co.kr/");
        headers.set("wanted_user_country", "KR");
        headers.set("wanted_user_language", "ko");
        return headers;
    }

    private String joinText(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(part.trim());
            }
        }
        return sb.toString();
    }
}
