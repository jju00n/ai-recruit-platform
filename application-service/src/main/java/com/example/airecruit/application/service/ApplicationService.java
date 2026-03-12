package com.example.airecruit.application.service;

import com.example.airecruit.application.client.JobServiceClient;
import com.example.airecruit.application.domain.AiFeedback;
import com.example.airecruit.application.domain.Application;
import com.example.airecruit.application.domain.Resume;
import com.example.airecruit.application.domain.enums.ApplicationStatus;
import com.example.airecruit.application.dto.AiJobReviewDto;
import com.example.airecruit.application.dto.ApplicationDto;
import com.example.airecruit.application.dto.ResumeDto;
import com.example.airecruit.application.kafka.ApplicationEventProducer;
import com.example.airecruit.application.kafka.ApplicationSubmittedEvent;
import com.example.airecruit.application.repository.AiFeedbackRepository;
import com.example.airecruit.application.repository.ApplicationRepository;
import com.example.airecruit.application.repository.ResumeRepository;
import com.example.airecruit.common.dto.Status;
import com.example.airecruit.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ResumeRepository resumeRepository;
    private final AiFeedbackRepository aiFeedbackRepository;
    private final ApplicationEventProducer eventProducer;
    private final RestClient jobServiceRestClient;
    private final JobServiceClient jobServiceClient;
    private final AiAnalysisService aiAnalysisService;

    @Transactional
    public ApplicationDto.Response submit(Long userId, ApplicationDto.SubmitReq req) {
        if (applicationRepository.existsByUserIdAndJobPostingId(userId, req.getJobPostingId())) {
            throw new BizException(Status.ALREADY_APPLIED);
        }

        Resume resume = resumeRepository.findByIdAndUserId(req.getResumeId(), userId)
                .orElseThrow(() -> new BizException(Status.RESUME_NOT_FOUND));

        Map<String, Object> jobData = fetchJobPosting(req.getJobPostingId());
        String jobTitle = (String) jobData.get("title");
        String jobDescription = (String) jobData.get("description");

        Application application = Application.builder()
                .userId(userId)
                .jobPostingId(req.getJobPostingId())
                .jobTitle(jobTitle)
                .jobDescription(jobDescription)
                .resumeId(resume.getId())
                .status(ApplicationStatus.APPLIED)
                .build();

        Application saved = applicationRepository.save(application);

        ApplicationSubmittedEvent event = ApplicationSubmittedEvent.builder()
                .applicationId(saved.getId())
                .userId(userId)
                .jobPostingId(req.getJobPostingId())
                .jobTitle(jobTitle)
                .jobDescription(jobDescription)
                .resumeContent(resume.getContent())
                .appliedAt(saved.getAppliedAt())
                .build();
        eventProducer.publishApplicationSubmitted(event);

        return ApplicationDto.Response.from(saved, null);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationDto.Response> getMyApplications(Long userId, int page, int size) {
        return applicationRepository
                .findAllByUserIdOrderByAppliedAtDesc(userId, PageRequest.of(page, size))
                .map(application -> {
                    Integer score = aiFeedbackRepository.findByApplicationId(application.getId())
                            .map(AiFeedback::getCompatibilityScore)
                            .orElse(null);
                    return ApplicationDto.Response.from(application, score);
                });
    }

    @Transactional(readOnly = true)
    public ApplicationDto.DetailResponse getApplicationDetail(Long userId, Long id) {
        Application application = applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BizException(Status.APPLICATION_NOT_FOUND));
        AiFeedback feedback = aiFeedbackRepository.findByApplicationId(id).orElse(null);
        return ApplicationDto.DetailResponse.from(application, feedback);
    }

    public AiJobReviewDto.Response getJobAiReview(Long userId, Long jobId) {
        // 사용자의 최신 이력서 조회
        List<Resume> resumes = resumeRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        if (resumes.isEmpty()) {
            throw new BizException(Status.RESUME_NOT_FOUND, "이력서를 먼저 등록해주세요.");
        }
        ResumeDto.Response resume = ResumeDto.Response.from(resumes.get(0));

        // 공고 상세 조회
        JobServiceClient.JobDetail job = jobServiceClient.getJobById(jobId);

        return aiAnalysisService.reviewJobFit(job, resume);
    }

    @Transactional
    public void withdraw(Long userId, Long id) {
        Application application = applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BizException(Status.APPLICATION_NOT_FOUND));
        if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new BizException(Status.FAIL, "이미 취소된 지원입니다.");
        }
        application.withdraw();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchJobPosting(Long jobPostingId) {
        try {
            Map<String, Object> response = jobServiceRestClient.get()
                    .uri("/api/v1/jobs/{id}", jobPostingId)
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                throw new BizException(Status.JOB_SERVICE_ERROR);
            }
            Object data = response.get("data");
            if (data instanceof Map) {
                return (Map<String, Object>) data;
            }
            throw new BizException(Status.JOB_POSTING_NOT_FOUND);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("[JobService] 공고 조회 실패 - jobPostingId={}, error={}", jobPostingId, e.getMessage());
            throw new BizException(Status.JOB_SERVICE_ERROR);
        }
    }
}
