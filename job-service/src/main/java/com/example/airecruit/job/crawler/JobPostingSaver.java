package com.example.airecruit.job.crawler;

import com.example.airecruit.job.document.JobPostingDocument;
import com.example.airecruit.job.domain.Company;
import com.example.airecruit.job.domain.JobPosting;
import com.example.airecruit.job.domain.enums.*;
import com.example.airecruit.job.repository.CompanyRepository;
import com.example.airecruit.job.repository.JobPostingRepository;
import com.example.airecruit.job.repository.JobPostingSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 크롤링 공고 저장 전용 서비스.
 * REQUIRES_NEW 전파 레벨로 공고 1건씩 독립 트랜잭션 처리.
 * 한 건 실패 시 해당 건만 롤백되고 다음 건 저장이 가능하도록 분리.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobPostingSaver {

    private final CompanyRepository companyRepository;
    private final JobPostingRepository jobPostingRepository;
    private final JobPostingSearchRepository jobPostingSearchRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOne(CrawledJobData data) {
        Company company = upsertCompany(data);
        JobPosting jobPosting = buildJobPosting(data, company);
        JobPosting saved = jobPostingRepository.save(jobPosting);
        indexToElasticsearch(saved, company.getName());
    }

    private Company upsertCompany(CrawledJobData data) {
        return companyRepository.findByName(data.getCompanyName())
                .orElseGet(() -> companyRepository.save(
                        Company.builder()
                                .name(data.getCompanyName())
                                .industry(data.getCompanyIndustry())
                                .location(data.getCompanyLocation())
                                .build()
                ));
    }

    private JobPosting buildJobPosting(CrawledJobData data, Company company) {
        LocalDate deadline = null;
        if (data.getDeadline() != null) {
            try { deadline = LocalDate.parse(data.getDeadline()); }
            catch (Exception ignored) {}
        }

        return JobPosting.builder()
                .company(company)
                .title(data.getTitle())
                .description(data.getDescription() != null ? data.getDescription() : data.getTitle())
                .responsibilities(data.getResponsibilities())
                .requirements(data.getRequirements())
                .preferredQualifications(data.getPreferredQualifications())
                .benefits(data.getBenefits())
                .hiringProcess(data.getHiringProcess())
                .jobCategory(mapJobCategory(data.getJobCategory()))
                .location(data.getLocation() != null ? data.getLocation() : "미정")
                .employmentType(mapEmploymentType(data.getEmploymentType()))
                .experienceLevel(mapExperienceLevel(data.getExperienceLevel()))
                .skills(data.getSkills())
                .deadline(deadline)
                .source(data.getSource())
                .sourceUrl(data.getSourceUrl())
                .build();
    }

    private void indexToElasticsearch(JobPosting jp, String companyName) {
        List<String> skillList = jp.getSkills() != null && !jp.getSkills().isBlank()
                ? Arrays.stream(jp.getSkills().split(",")).map(String::trim).toList()
                : Collections.emptyList();

        JobPostingDocument doc = JobPostingDocument.builder()
                .id(jp.getId().toString())
                .jobPostingId(jp.getId())
                .companyId(jp.getCompany().getId())
                .companyName(companyName)
                .title(jp.getTitle())
                .description(jp.getDescription())
                .jobCategory(jp.getJobCategory().name())
                .location(jp.getLocation())
                .employmentType(jp.getEmploymentType().name())
                .experienceLevel(jp.getExperienceLevel().name())
                .minSalary(jp.getMinSalary())
                .maxSalary(jp.getMaxSalary())
                .skills(skillList)
                .status(jp.getStatus().name())
                .source(jp.getSource().name())
                .deadline(jp.getDeadline() != null ? jp.getDeadline().toString() : null)
                .createdAt(jp.getCreatedAt() != null ? jp.getCreatedAt() : LocalDateTime.now())
                .build();

        jobPostingSearchRepository.save(doc);
    }

    // ── enum 매핑 ──────────────────────────────────────────────────────────

    private static final java.util.Map<String, JobCategory> JOB_CATEGORY_MAP = java.util.Map.ofEntries(
            java.util.Map.entry("백엔드", JobCategory.BACKEND),
            java.util.Map.entry("서버", JobCategory.BACKEND),
            java.util.Map.entry("프론트엔드", JobCategory.FRONTEND),
            java.util.Map.entry("프론트", JobCategory.FRONTEND),
            java.util.Map.entry("풀스택", JobCategory.FULLSTACK),
            java.util.Map.entry("devops", JobCategory.DEVOPS),
            java.util.Map.entry("데브옵스", JobCategory.DEVOPS),
            java.util.Map.entry("인프라", JobCategory.DEVOPS),
            java.util.Map.entry("데이터 엔지니어", JobCategory.DATA_ENGINEER),
            java.util.Map.entry("데이터", JobCategory.DATA_ENGINEER),
            java.util.Map.entry("머신러닝", JobCategory.ML_ENGINEER),
            java.util.Map.entry("ai", JobCategory.ML_ENGINEER),
            java.util.Map.entry("ios", JobCategory.MOBILE_IOS),
            java.util.Map.entry("android", JobCategory.MOBILE_ANDROID),
            java.util.Map.entry("안드로이드", JobCategory.MOBILE_ANDROID),
            java.util.Map.entry("디자인", JobCategory.DESIGN),
            java.util.Map.entry("pm", JobCategory.PM),
            java.util.Map.entry("기획", JobCategory.PM),
            java.util.Map.entry("qa", JobCategory.QA),
            java.util.Map.entry("개발", JobCategory.BACKEND)
    );

    JobCategory mapJobCategory(String raw) {
        if (raw == null) return JobCategory.ETC;
        String lower = raw.toLowerCase().trim();
        return JOB_CATEGORY_MAP.entrySet().stream()
                .filter(e -> lower.contains(e.getKey()))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElse(JobCategory.ETC);
    }

    EmploymentType mapEmploymentType(String raw) {
        if (raw == null) return EmploymentType.FULL_TIME;
        String lower = raw.toLowerCase();
        if (lower.contains("계약") || lower.contains("contract")) return EmploymentType.CONTRACT;
        if (lower.contains("인턴") || lower.contains("intern")) return EmploymentType.INTERN;
        if (lower.contains("파트") || lower.contains("part")) return EmploymentType.PART_TIME;
        return EmploymentType.FULL_TIME;
    }

    ExperienceLevel mapExperienceLevel(String raw) {
        if (raw == null) return ExperienceLevel.NEWCOMER;
        String lower = raw.toLowerCase();
        if (lower.contains("신입")) return ExperienceLevel.NEWCOMER;
        if (lower.contains("8년") || lower.contains("10년") || lower.contains("리드") ||
                lower.contains("수석") || lower.contains("principal")) return ExperienceLevel.LEAD;
        if (lower.contains("4년") || lower.contains("5년") || lower.contains("6년") ||
                lower.contains("7년") || lower.contains("시니어") || lower.contains("senior")) return ExperienceLevel.SENIOR;
        if (lower.contains("1년") || lower.contains("2년") || lower.contains("3년") ||
                lower.contains("주니어") || lower.contains("junior")) return ExperienceLevel.JUNIOR;
        if (lower.contains("경력")) return ExperienceLevel.JUNIOR;
        return ExperienceLevel.NEWCOMER;
    }
}
