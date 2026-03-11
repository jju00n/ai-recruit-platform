package com.example.airecruit.application.controller;

import com.example.airecruit.application.client.JobServiceClient;
import com.example.airecruit.application.dto.AiCoachingDto;
import com.example.airecruit.application.dto.AiJobRecommendationDto;
import com.example.airecruit.application.dto.ResumeDto;
import com.example.airecruit.application.service.AiAnalysisService;
import com.example.airecruit.application.service.ResumeService;
import com.example.airecruit.common.dto.ResultData;
import com.example.airecruit.common.dto.Status;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final AiAnalysisService aiAnalysisService;
    private final JobServiceClient jobServiceClient;

    @PostMapping
    public ResponseEntity<ResultData<ResumeDto.Response>> createTextResume(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid ResumeDto.TextCreateReq req) {
        ResumeDto.Response response = resumeService.createTextResume(userId, req);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }

    @PostMapping("/upload")
    public ResponseEntity<ResultData<ResumeDto.Response>> uploadPdfResume(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title) {
        ResumeDto.Response response = resumeService.uploadPdfResume(userId, file, title);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }

    @GetMapping
    public ResponseEntity<ResultData<List<ResumeDto.Response>>> getMyResumes(
            @RequestHeader("X-User-Id") Long userId) {
        List<ResumeDto.Response> resumes = resumeService.getMyResumes(userId);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, resumes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResultData<ResumeDto.Response>> getResume(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        ResumeDto.Response resume = resumeService.getResume(userId, id);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, resume));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResultData<ResumeDto.Response>> updateResume(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @RequestBody @Valid ResumeDto.TextUpdateReq req) {
        ResumeDto.Response response = resumeService.updateResume(userId, id, req);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResultData<Void>> deleteResume(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        resumeService.deleteResume(userId, id);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS));
    }

    @PostMapping("/{id}/coaching")
    public ResponseEntity<ResultData<AiCoachingDto.Response>> coachResume(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        ResumeDto.Response resume = resumeService.getResume(userId, id);
        AiCoachingDto.Response coaching = aiAnalysisService.coachResume(resume);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, coaching));
    }

    @GetMapping("/{id}/recommended-jobs")
    public ResponseEntity<ResultData<List<AiJobRecommendationDto.Response>>> getRecommendedJobs(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        ResumeDto.Response resume = resumeService.getResume(userId, id);
        List<JobServiceClient.JobSummary> jobs = jobServiceClient.getJobs();
        List<AiJobRecommendationDto.Response> recommendations = aiAnalysisService.recommendJobs(resume, jobs);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, recommendations));
    }
}
