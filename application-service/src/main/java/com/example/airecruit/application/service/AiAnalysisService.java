package com.example.airecruit.application.service;

import com.example.airecruit.application.client.JobServiceClient;
import com.example.airecruit.application.domain.AiFeedback;
import com.example.airecruit.application.dto.AiCoachingDto;
import com.example.airecruit.application.dto.AiJobRecommendationDto;
import com.example.airecruit.application.dto.ResumeDto;
import com.example.airecruit.application.kafka.ApplicationSubmittedEvent;
import com.example.airecruit.application.kafka.ApplicationEventProducer;
import com.example.airecruit.application.kafka.ResumeAnalyzedEvent;
import com.example.airecruit.application.repository.AiFeedbackRepository;
import com.example.airecruit.common.dto.Status;
import com.example.airecruit.common.exception.BizException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisService {

    private final AiFeedbackRepository aiFeedbackRepository;
    private final ApplicationEventProducer eventProducer;
    private final ObjectMapper objectMapper;
    private final RestClient claudeRestClient;
    private final StringRedisTemplate redisTemplate;

    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final String CACHE_COACHING = "ai:coaching:";
    private static final String CACHE_RECOMMEND = "ai:recommend:";

    @Value("${claude.api.model}")
    private String claudeModel;

    @Value("${claude.api.fast-model}")
    private String claudeFastModel;

    // ── 기술 키워드 목록 (사전 필터링용) ──────────────────────────────────────
    private static final List<String> TECH_KEYWORDS = List.of(
            "java", "spring", "python", "javascript", "typescript", "react", "vue", "angular",
            "node", "kotlin", "go", "rust", "c++", "c#", "aws", "docker", "kubernetes",
            "mysql", "postgresql", "mongodb", "redis", "kafka", "elasticsearch",
            "백엔드", "프론트엔드", "풀스택", "devops", "데이터", "머신러닝", "ai", "ml",
            "안드로이드", "ios", "flutter", "swift", "android"
    );

    // ── 시스템 프롬프트 (캐싱 대상) ───────────────────────────────────────────
    private static final String COACHING_SYSTEM = """
            당신은 채용 컨설턴트입니다. 이력서를 분석하고 반드시 JSON만 응답하세요.
            다른 텍스트, 설명, 마크다운 코드 펜스 없이 순수 JSON만 출력하세요.
            """;

    private static final String MATCHING_SYSTEM = """
            당신은 채용 매칭 전문가입니다. 이력서와 채용공고를 비교하여 반드시 JSON만 응답하세요.
            다른 텍스트, 설명, 마크다운 코드 펜스 없이 순수 JSON만 출력하세요.
            """;

    private static final String ANALYSIS_SYSTEM = """
            당신은 채용 전문가입니다. 채용공고와 이력서를 분석하여 반드시 JSON만 응답하세요.
            다른 텍스트, 설명, 마크다운 코드 펜스 없이 순수 JSON만 출력하세요.
            """;

    // ── Kafka 이벤트 기반 AI 분석 ─────────────────────────────────────────────
    public void analyze(ApplicationSubmittedEvent event) {
        log.info("[AI] 분석 시작 - applicationId={}", event.getApplicationId());

        String userContent = String.format("""
                [채용공고]
                제목: %s
                내용: %s

                [이력서]
                %s

                아래 JSON 형식으로만 응답하세요:
                {"compatibilityScore":75,"summary":"전반적인 평가","strengths":"강점","weaknesses":"약점","recommendation":"개선 방법"}
                compatibilityScore는 0~100 사이의 정수입니다.
                """, event.getJobTitle(), abbreviate(event.getJobDescription(), 1500), abbreviate(event.getResumeContent(), 2000));

        try {
            String responseText = callClaudeApiWithSystem(claudeModel, ANALYSIS_SYSTEM, userContent);
            AiFeedback feedback = parseAndSave(event.getApplicationId(), responseText);

            ResumeAnalyzedEvent analyzedEvent = ResumeAnalyzedEvent.builder()
                    .applicationId(event.getApplicationId())
                    .userId(event.getUserId())
                    .jobPostingId(event.getJobPostingId())
                    .jobTitle(event.getJobTitle())
                    .compatibilityScore(feedback.getCompatibilityScore())
                    .summary(feedback.getSummary())
                    .analyzedAt(feedback.getAnalyzedAt())
                    .build();
            eventProducer.publishResumeAnalyzed(analyzedEvent);

            log.info("[AI] 분석 완료 - applicationId={}, score={}", event.getApplicationId(), feedback.getCompatibilityScore());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AI] 분석 실패 - applicationId={}, error={}", event.getApplicationId(), e.getMessage(), e);
            throw new BizException(Status.AI_ANALYSIS_ERROR);
        }
    }

    // ── AI 코칭 ───────────────────────────────────────────────────────────────
    public AiCoachingDto.Response coachResume(ResumeDto.Response resume) {
        log.info("[AI 코칭] 시작 - resumeId={}", resume.getId());

        // 캐시 조회
        String cacheKey = CACHE_COACHING + resume.getId();
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                log.info("[AI 코칭] 캐시 히트 - resumeId={}", resume.getId());
                return objectMapper.readValue(cached, AiCoachingDto.Response.class);
            } catch (Exception e) {
                log.warn("[AI 코칭] 캐시 역직렬화 실패, Claude 호출로 대체");
            }
        }

        String userContent = String.format("""
                [이력서]
                %s

                아래 JSON 형식으로만 응답하세요:
                {"overallScore":72,"summary":"요약 (100자 이내)","structuralFeedback":"구조 피드백 (100자 이내)","contentFeedback":"내용 피드백 (100자 이내)","improvements":[{"section":"섹션명","issue":"문제점","suggestion":"개선 제안","example":"예시"}]}
                improvements는 정확히 3개, 각 필드 80자 이내.
                """, abbreviate(resume.getContent(), 3000));

        try {
            String responseText = callClaudeApiWithSystem(claudeFastModel, COACHING_SYSTEM, userContent);
            String json = stripCodeFence(responseText);
            JsonNode node = objectMapper.readTree(json);

            List<AiCoachingDto.Improvement> improvements = new ArrayList<>();
            JsonNode improvNode = node.path("improvements");
            if (improvNode.isArray()) {
                for (JsonNode imp : improvNode) {
                    improvements.add(new AiCoachingDto.Improvement(
                            imp.path("section").asText(""),
                            imp.path("issue").asText(""),
                            imp.path("suggestion").asText(""),
                            imp.path("example").asText("")
                    ));
                }
            }

            AiCoachingDto.Response result = new AiCoachingDto.Response(
                    node.path("overallScore").asInt(50),
                    node.path("summary").asText(""),
                    node.path("structuralFeedback").asText(""),
                    node.path("contentFeedback").asText(""),
                    improvements
            );

            // 캐시 저장
            try {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), CACHE_TTL);
            } catch (Exception e) {
                log.warn("[AI 코칭] 캐시 저장 실패 (무시): {}", e.getMessage());
            }

            log.info("[AI 코칭] 완료 - resumeId={}, score={}", resume.getId(), result.overallScore());
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AI 코칭] 실패 - resumeId={}, error={}", resume.getId(), e.getMessage(), e);
            throw new BizException(Status.RESUME_COACHING_ERROR);
        }
    }

    // ── AI 공고 추천 ──────────────────────────────────────────────────────────
    public List<AiJobRecommendationDto.Response> recommendJobs(ResumeDto.Response resume, List<JobServiceClient.JobSummary> jobs) {
        log.info("[AI 추천] 시작 - resumeId={}, jobCount={}", resume.getId(), jobs.size());

        // 캐시 조회
        String cacheKey = CACHE_RECOMMEND + resume.getId();
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                log.info("[AI 추천] 캐시 히트 - resumeId={}", resume.getId());
                return objectMapper.readValue(cached, new TypeReference<List<AiJobRecommendationDto.Response>>() {});
            } catch (Exception e) {
                log.warn("[AI 추천] 캐시 역직렬화 실패, Claude 호출로 대체");
            }
        }

        // ① 이력서 키워드 추출 → 관련 공고 상위 8개로 사전 필터링
        List<JobServiceClient.JobSummary> filteredJobs = preFilterJobs(resume.getContent(), jobs);
        log.info("[AI 추천] 필터링 후 공고 수: {}", filteredJobs.size());

        // ② 공고 목록 (캐싱 대상 블록 — 고정 데이터)
        String jobListText = filteredJobs.stream()
                .map(j -> String.format("{\"id\":%d,\"title\":\"%s\",\"company\":\"%s\",\"desc\":\"%s\"}",
                        j.id(), j.title(), j.companyName(), abbreviate(j.description(), 150)))
                .collect(Collectors.joining(",\n", "[채용공고 목록]\n[", "]"));

        // ③ 이력서 (가변 데이터 — 캐싱 미적용)
        String resumeText = "[이력서]\n" + abbreviate(resume.getContent(), 1500);

        String instruction = """

                위 이력서와 채용공고를 비교해 가장 적합한 공고 상위 5개를 선별하세요.
                JSON 배열로만 응답하세요:
                [{"jobPostingId":1,"jobTitle":"백엔드 개발자","companyName":"회사명","matchScore":85,"matchReason":"Spring Boot 경험이 JD와 일치"}]
                matchScore는 0~100 사이의 정수입니다.
                """;

        try {
            // ④ 공고 목록(캐싱) + 이력서(가변)를 content 블록으로 분리
            String responseText = callClaudeApiWithCache(claudeFastModel, MATCHING_SYSTEM, jobListText, resumeText + instruction);
            String json = stripCodeFence(responseText);
            JsonNode arrayNode = objectMapper.readTree(json);

            List<AiJobRecommendationDto.Response> result = new ArrayList<>();
            if (arrayNode.isArray()) {
                for (JsonNode node : arrayNode) {
                    Long jobId = node.path("jobPostingId").asLong();
                    String title = node.path("jobTitle").asText("");
                    String company = node.path("companyName").asText("");
                    for (JobServiceClient.JobSummary j : filteredJobs) {
                        if (j.id().equals(jobId)) {
                            if (title.isBlank()) title = j.title();
                            if (company.isBlank()) company = j.companyName();
                            break;
                        }
                    }
                    result.add(new AiJobRecommendationDto.Response(
                            jobId, title, company,
                            node.path("matchScore").asInt(0),
                            node.path("matchReason").asText("")
                    ));
                }
            }
            // 캐시 저장
            try {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), CACHE_TTL);
            } catch (Exception e) {
                log.warn("[AI 추천] 캐시 저장 실패 (무시): {}", e.getMessage());
            }

            log.info("[AI 추천] 완료 - resumeId={}, 추천수={}", resume.getId(), result.size());
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AI 추천] 실패 - resumeId={}, error={}", resume.getId(), e.getMessage(), e);
            throw new BizException(Status.AI_ANALYSIS_ERROR);
        }
    }

    // ── Claude API 호출 (system prompt + cache_control) ───────────────────────

    /**
     * system: 캐싱 적용, user: 단일 블록
     */
    private String callClaudeApiWithSystem(String model, String systemPrompt, String userContent) {
        return callClaudeApiWithSystem(model, systemPrompt, userContent, 1024);
    }

    private String callClaudeApiWithSystem(String model, String systemPrompt, String userContent, int maxTokens) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("system", List.of(
                Map.of("type", "text", "text", systemPrompt,
                        "cache_control", Map.of("type", "ephemeral"))
        ));
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", userContent)
        ));

        return extractText(invoke(requestBody));
    }

    /**
     * system: 캐싱 적용, user: [cachedBlock (cache_control), variableBlock]
     * cachedBlock — 공고 목록처럼 고정 데이터
     * variableBlock — 이력서처럼 사용자마다 다른 데이터
     */
    private String callClaudeApiWithCache(String model, String systemPrompt, String cachedContent, String variableContent) {
        return callClaudeApiWithCache(model, systemPrompt, cachedContent, variableContent, 1024);
    }

    private String callClaudeApiWithCache(String model, String systemPrompt, String cachedContent, String variableContent, int maxTokens) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("system", List.of(
                Map.of("type", "text", "text", systemPrompt,
                        "cache_control", Map.of("type", "ephemeral"))
        ));

        List<Map<String, Object>> contentBlocks = List.of(
                // 고정 데이터 → 캐싱
                Map.of("type", "text", "text", cachedContent,
                        "cache_control", Map.of("type", "ephemeral")),
                // 가변 데이터 → 캐싱 없음
                Map.of("type", "text", "text", variableContent)
        );

        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", contentBlocks)
        ));

        return extractText(invoke(requestBody));
    }

    private Map<?, ?> invoke(Map<String, Object> requestBody) {
        int maxRetries = 3;
        int delayMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Map<?, ?> response = claudeRestClient.post()
                        .body(requestBody)
                        .retrieve()
                        .body(Map.class);

                if (response == null) {
                    throw new BizException(Status.AI_ANALYSIS_ERROR);
                }
                return response;

            } catch (org.springframework.web.client.HttpServerErrorException e) {
                if (e.getStatusCode().value() == 529 && attempt < maxRetries) {
                    log.warn("[Claude API] 과부하(529) - {}번째 재시도 ({}ms 대기)", attempt, delayMs);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BizException(Status.AI_ANALYSIS_ERROR);
                    }
                    delayMs *= 2; // exponential backoff
                } else {
                    throw e;
                }
            }
        }
        throw new BizException(Status.AI_ANALYSIS_ERROR);
    }

    private String extractText(Map<?, ?> response) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        if (content == null || content.isEmpty()) {
            throw new BizException(Status.AI_ANALYSIS_ERROR);
        }
        return (String) content.get(0).get("text");
    }

    // ── 사전 필터링 ───────────────────────────────────────────────────────────

    /**
     * 이력서에서 기술 키워드를 추출한 뒤, 공고 title+description과 키워드 매칭 점수를 계산해
     * 상위 topN개만 반환합니다.
     */
    private List<JobServiceClient.JobSummary> preFilterJobs(String resumeContent, List<JobServiceClient.JobSummary> jobs) {
        final int topN = 8;
        if (jobs.size() <= topN) return jobs;

        String resumeLower = resumeContent == null ? "" : resumeContent.toLowerCase();
        List<String> matchedKeywords = TECH_KEYWORDS.stream()
                .filter(resumeLower::contains)
                .toList();

        if (matchedKeywords.isEmpty()) {
            return jobs.subList(0, topN);
        }

        return jobs.stream()
                .map(job -> {
                    String target = (job.title() + " " + job.description()).toLowerCase();
                    long score = matchedKeywords.stream().filter(target::contains).count();
                    return Map.entry(job, score);
                })
                .sorted(Map.Entry.<JobServiceClient.JobSummary, Long>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .toList();
    }

    // ── 유틸 ─────────────────────────────────────────────────────────────────

    private String stripCodeFence(String text) {
        String result = text.trim();
        if (result.contains("```")) {
            result = result.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        }
        return result;
    }

    private String abbreviate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private AiFeedback parseAndSave(Long applicationId, String responseText) {
        try {
            String jsonText = stripCodeFence(responseText);
            JsonNode node = objectMapper.readTree(jsonText);
            AiFeedback feedback = AiFeedback.builder()
                    .applicationId(applicationId)
                    .compatibilityScore(node.get("compatibilityScore").asInt())
                    .summary(node.get("summary").asText())
                    .strengths(node.get("strengths").asText())
                    .weaknesses(node.get("weaknesses").asText())
                    .recommendation(node.get("recommendation").asText())
                    .analyzedAt(LocalDateTime.now())
                    .build();
            return aiFeedbackRepository.save(feedback);
        } catch (Exception e) {
            log.error("[AI] 응답 파싱 실패: {}", e.getMessage());
            throw new BizException(Status.AI_ANALYSIS_ERROR, "AI 응답 파싱에 실패했습니다.");
        }
    }
}
