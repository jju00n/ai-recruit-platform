package com.example.airecruit.application.controller;

import com.example.airecruit.application.dto.AiJobReviewDto;
import com.example.airecruit.application.dto.ApplicationDto;
import com.example.airecruit.application.service.ApplicationService;
import com.example.airecruit.common.dto.ResultData;
import com.example.airecruit.common.dto.Status;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ResultData<ApplicationDto.Response>> submit(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid ApplicationDto.SubmitReq req) {
        ApplicationDto.Response response = applicationService.submit(userId, req);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }

    @GetMapping
    public ResponseEntity<ResultData<Page<ApplicationDto.Response>>> getMyApplications(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ApplicationDto.Response> applications = applicationService.getMyApplications(userId, page, size);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, applications));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResultData<ApplicationDto.DetailResponse>> getApplicationDetail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        ApplicationDto.DetailResponse response = applicationService.getApplicationDetail(userId, id);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }

    @GetMapping("/jobs/{jobId}/ai-review")
    public ResponseEntity<ResultData<AiJobReviewDto.Response>> getJobAiReview(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long jobId) {
        AiJobReviewDto.Response response = applicationService.getJobAiReview(userId, jobId);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResultData<Void>> withdraw(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        applicationService.withdraw(userId, id);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS));
    }
}
