package com.example.airecruit.job.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.example.airecruit.job.document.JobPostingDocument;
import com.example.airecruit.job.dto.JobPostingDto;
import com.example.airecruit.job.dto.JobSearchDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public Page<JobPostingDto.SearchResponse> search(JobSearchDto dto) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // 항상 OPEN 공고만 조회
        boolQuery.filter(Query.of(q -> q.term(t -> t.field("status").value("OPEN"))));

        // 키워드 검색 (title, description, skills 대상 multi_match)
        if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
            boolQuery.must(Query.of(q -> q.multiMatch(m -> m
                    .fields("title", "description", "skills")
                    .query(dto.getKeyword())
            )));
        }

        // 직무 카테고리 필터
        if (dto.getJobCategory() != null) {
            boolQuery.filter(Query.of(q -> q.term(t -> t
                    .field("jobCategory").value(dto.getJobCategory().name())
            )));
        }

        // 지역 필터
        if (dto.getLocation() != null && !dto.getLocation().isBlank()) {
            boolQuery.filter(Query.of(q -> q.term(t -> t
                    .field("location").value(dto.getLocation())
            )));
        }

        // 고용형태 필터
        if (dto.getEmploymentType() != null) {
            boolQuery.filter(Query.of(q -> q.term(t -> t
                    .field("employmentType").value(dto.getEmploymentType().name())
            )));
        }

        // 경력 필터
        if (dto.getExperienceLevel() != null) {
            boolQuery.filter(Query.of(q -> q.term(t -> t
                    .field("experienceLevel").value(dto.getExperienceLevel().name())
            )));
        }

        // 최소 연봉 필터 (maxSalary >= minSalary 조건)
        if (dto.getMinSalary() != null) {
            boolQuery.filter(Query.of(q -> q.range(r -> r
                    .field("maxSalary")
                    .gte(JsonData.of(dto.getMinSalary()))
            )));
        }

        PageRequest pageable = PageRequest.of(dto.getPage(), dto.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolQuery.build())))
                .withPageable(pageable)
                .build();

        SearchHits<JobPostingDocument> hits = elasticsearchOperations.search(query, JobPostingDocument.class);

        List<JobPostingDto.SearchResponse> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toSearchResponse)
                .toList();

        log.info("[JobSearchService] 검색 결과 - keyword={}, total={}", dto.getKeyword(), hits.getTotalHits());
        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }

    private JobPostingDto.SearchResponse toSearchResponse(JobPostingDocument doc) {
        LocalDate deadline = doc.getDeadline() != null ? LocalDate.parse(doc.getDeadline()) : null;

        return JobPostingDto.SearchResponse.builder()
                .id(doc.getJobPostingId())
                .companyId(doc.getCompanyId())
                .companyName(doc.getCompanyName())
                .title(doc.getTitle())
                .jobCategory(doc.getJobCategory() != null
                        ? com.example.airecruit.job.domain.enums.JobCategory.valueOf(doc.getJobCategory()) : null)
                .location(doc.getLocation())
                .employmentType(doc.getEmploymentType() != null
                        ? com.example.airecruit.job.domain.enums.EmploymentType.valueOf(doc.getEmploymentType()) : null)
                .experienceLevel(doc.getExperienceLevel() != null
                        ? com.example.airecruit.job.domain.enums.ExperienceLevel.valueOf(doc.getExperienceLevel()) : null)
                .minSalary(doc.getMinSalary())
                .maxSalary(doc.getMaxSalary())
                .skills(doc.getSkills() != null ? doc.getSkills() : Collections.emptyList())
                .deadline(deadline)
                .build();
    }
}
