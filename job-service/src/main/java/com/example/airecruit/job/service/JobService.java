package com.example.airecruit.job.service;

import com.example.airecruit.common.dto.Status;
import com.example.airecruit.common.exception.BizException;
import com.example.airecruit.job.document.JobPostingDocument;
import com.example.airecruit.job.domain.Company;
import com.example.airecruit.job.domain.JobPosting;
import com.example.airecruit.job.domain.enums.JobStatus;
import com.example.airecruit.job.dto.CompanyDto;
import com.example.airecruit.job.dto.JobPostingDto;
import com.example.airecruit.job.kafka.JobCreatedEvent;
import com.example.airecruit.job.kafka.JobEventProducer;
import com.example.airecruit.job.repository.CompanyRepository;
import com.example.airecruit.job.repository.JobPostingRepository;
import com.example.airecruit.job.repository.JobPostingSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final CompanyRepository companyRepository;
    private final JobPostingRepository jobPostingRepository;
    private final JobPostingSearchRepository jobPostingSearchRepository;
    private final JobEventProducer jobEventProducer;
    private final EmbeddingService embeddingService;

    @Transactional
    public CompanyDto.Response createCompany(CompanyDto.CreateReq req) {
        Company company = Company.builder()
                .name(req.getName())
                .industry(req.getIndustry())
                .size(req.getSize())
                .location(req.getLocation())
                .description(req.getDescription())
                .website(req.getWebsite())
                .build();
        return CompanyDto.Response.from(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public CompanyDto.Response getCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BizException(Status.COMPANY_NOT_FOUND));
        return CompanyDto.Response.from(company);
    }

    @Transactional
    public JobPostingDto.Response createJobPosting(JobPostingDto.CreateReq req) {
        Company company = companyRepository.findById(req.getCompanyId())
                .orElseThrow(() -> new BizException(Status.COMPANY_NOT_FOUND));

        JobPosting jobPosting = JobPosting.builder()
                .company(company)
                .title(req.getTitle())
                .description(req.getDescription())
                .responsibilities(req.getResponsibilities())
                .requirements(req.getRequirements())
                .preferredQualifications(req.getPreferredQualifications())
                .benefits(req.getBenefits())
                .hiringProcess(req.getHiringProcess())
                .jobCategory(req.getJobCategory())
                .location(req.getLocation())
                .employmentType(req.getEmploymentType())
                .experienceLevel(req.getExperienceLevel())
                .minSalary(req.getMinSalary())
                .maxSalary(req.getMaxSalary())
                .skills(req.getSkills())
                .deadline(req.getDeadline())
                .build();

        JobPosting saved = jobPostingRepository.save(jobPosting);

        indexToElasticsearch(saved, company.getName());

        jobEventProducer.publishJobCreated(JobCreatedEvent.builder()
                .jobPostingId(saved.getId())
                .companyId(company.getId())
                .companyName(company.getName())
                .title(saved.getTitle())
                .jobCategory(saved.getJobCategory().name())
                .location(saved.getLocation())
                .createdAt(saved.getCreatedAt())
                .build());

        log.info("[JobService] 채용공고 등록 완료 - id={}, title={}", saved.getId(), saved.getTitle());
        return JobPostingDto.Response.from(saved);
    }

    @Transactional(readOnly = true)
    public JobPostingDto.Response getJobPosting(Long jobPostingId) {
        JobPosting jobPosting = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new BizException(Status.JOB_POSTING_NOT_FOUND));
        return JobPostingDto.Response.from(jobPosting);
    }

    @Transactional(readOnly = true)
    public Page<JobPostingDto.Response> getJobPostings(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return jobPostingRepository.findAllByStatusWithCompany(JobStatus.OPEN, pageable)
                .map(JobPostingDto.Response::from);
    }

    @Transactional
    public JobPostingDto.Response updateJobPosting(Long jobPostingId, JobPostingDto.UpdateReq req) {
        JobPosting jobPosting = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new BizException(Status.JOB_POSTING_NOT_FOUND));

        jobPosting.update(
                req.getTitle(), req.getDescription(),
                req.getResponsibilities(), req.getRequirements(),
                req.getPreferredQualifications(), req.getBenefits(), req.getHiringProcess(),
                req.getJobCategory(), req.getLocation(),
                req.getEmploymentType(), req.getExperienceLevel(),
                req.getMinSalary(), req.getMaxSalary(), req.getSkills(),
                req.getStatus(), req.getDeadline()
        );

        indexToElasticsearch(jobPosting, jobPosting.getCompany().getName());

        log.info("[JobService] 채용공고 수정 완료 - id={}", jobPostingId);
        return JobPostingDto.Response.from(jobPosting);
    }

    @Transactional
    public void closeJobPosting(Long jobPostingId) {
        JobPosting jobPosting = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new BizException(Status.JOB_POSTING_NOT_FOUND));

        jobPosting.close();
        indexToElasticsearch(jobPosting, jobPosting.getCompany().getName());

        log.info("[JobService] 채용공고 마감 처리 완료 - id={}", jobPostingId);
    }

    @Transactional(readOnly = true)
    public int reindexAll() {
        List<JobPosting> all = jobPostingRepository.findAllWithCompany();
        for (JobPosting jp : all) {
            indexToElasticsearch(jp, jp.getCompany().getName());
        }
        log.info("[JobService] ES 전체 재색인 완료 - {}건", all.size());
        return all.size();
    }

    private void indexToElasticsearch(JobPosting jobPosting, String companyName) {
        List<String> skillList = parseSkills(jobPosting.getSkills());
        String deadline = jobPosting.getDeadline() != null ? jobPosting.getDeadline().toString() : null;

        float[] vector = embeddingService.embed(buildEmbeddingText(jobPosting));

        JobPostingDocument document = JobPostingDocument.builder()
                .id(jobPosting.getId().toString())
                .jobPostingId(jobPosting.getId())
                .companyId(jobPosting.getCompany().getId())
                .companyName(companyName)
                .title(jobPosting.getTitle())
                .description(jobPosting.getDescription())
                .jobCategory(jobPosting.getJobCategory().name())
                .location(jobPosting.getLocation())
                .employmentType(jobPosting.getEmploymentType().name())
                .experienceLevel(jobPosting.getExperienceLevel().name())
                .minSalary(jobPosting.getMinSalary())
                .maxSalary(jobPosting.getMaxSalary())
                .skills(skillList)
                .status(jobPosting.getStatus().name())
                .source(jobPosting.getSource().name())
                .deadline(deadline)
                .createdAt(jobPosting.getCreatedAt())
                .descriptionVector(vector)
                .build();

        jobPostingSearchRepository.save(document);
    }

    private String buildEmbeddingText(JobPosting jp) {
        return String.join(" ",
                jp.getTitle() != null ? jp.getTitle() : "",
                jp.getDescription() != null ? jp.getDescription() : "",
                jp.getRequirements() != null ? jp.getRequirements() : "",
                jp.getSkills() != null ? jp.getSkills() : ""
        );
    }

    private List<String> parseSkills(String skills) {
        if (skills == null || skills.isBlank()) return Collections.emptyList();
        return Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
