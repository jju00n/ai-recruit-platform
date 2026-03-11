package com.example.airecruit.job.controller;

import com.example.airecruit.common.dto.ResultData;
import com.example.airecruit.common.dto.Status;
import com.example.airecruit.job.crawler.JobCrawlService;
import com.example.airecruit.job.dto.CompanyDto;
import com.example.airecruit.job.dto.JobPostingDto;
import com.example.airecruit.job.dto.JobSearchDto;
import com.example.airecruit.job.service.JobSearchService;
import com.example.airecruit.job.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobSearchService jobSearchService;
    private final JobCrawlService jobCrawlService;

    // 회사 등록
    @PostMapping("/companies")
    public ResponseEntity<ResultData<CompanyDto.Response>> createCompany(
            @Valid @RequestBody CompanyDto.CreateReq req) {
        CompanyDto.Response response = jobService.createCompany(req);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }

    // 회사 단건 조회
    @GetMapping("/companies/{id}")
    public ResponseEntity<ResultData<CompanyDto.Response>> getCompany(@PathVariable Long id) {
        CompanyDto.Response response = jobService.getCompany(id);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }

    // 채용공고 등록
    @PostMapping
    public ResponseEntity<ResultData<JobPostingDto.Response>> createJobPosting(
            @Valid @RequestBody JobPostingDto.CreateReq req) {
        JobPostingDto.Response response = jobService.createJobPosting(req);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }

    // 채용공고 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<ResultData<JobPostingDto.Response>> getJobPosting(@PathVariable Long id) {
        JobPostingDto.Response response = jobService.getJobPosting(id);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }

    // 채용공고 목록 조회 (MySQL, 페이징)
    @GetMapping
    public ResponseEntity<ResultData<Page<JobPostingDto.Response>>> getJobPostings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<JobPostingDto.Response> response = jobService.getJobPostings(page, size);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }

    // 채용공고 수정
    @PutMapping("/{id}")
    public ResponseEntity<ResultData<JobPostingDto.Response>> updateJobPosting(
            @PathVariable Long id,
            @Valid @RequestBody JobPostingDto.UpdateReq req) {
        JobPostingDto.Response response = jobService.updateJobPosting(id, req);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }

    // 채용공고 마감 (소프트 삭제)
    @DeleteMapping("/{id}")
    public ResponseEntity<ResultData<?>> closeJobPosting(@PathVariable Long id) {
        jobService.closeJobPosting(id);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS));
    }

    // 채용공고 검색 (Elasticsearch)
    @GetMapping("/search")
    public ResponseEntity<ResultData<Page<JobPostingDto.SearchResponse>>> searchJobPostings(
            JobSearchDto dto) {
        Page<JobPostingDto.SearchResponse> response = jobSearchService.search(dto);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }

    // ES 전체 재색인 (인덱스 매핑 변경 후 사용)
    @PostMapping("/reindex")
    public ResponseEntity<ResultData<String>> reindexAll() {
        int count = jobService.reindexAll();
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, "재색인 완료: " + count + "건"));
    }

    // 크롤링 수동 트리거 (개발/데모용)
    @PostMapping("/crawl")
    public ResponseEntity<ResultData<String>> triggerCrawl(
            @RequestParam(defaultValue = "all") String source) {
        JobCrawlService.CrawlResult result = switch (source.toLowerCase()) {
            case "wanted"  -> jobCrawlService.crawlWanted();
            case "saramin" -> jobCrawlService.crawlSaramin();
            default -> {
                JobCrawlService.CrawlResult w = jobCrawlService.crawlWanted();
                JobCrawlService.CrawlResult s = jobCrawlService.crawlSaramin();
                yield new JobCrawlService.CrawlResult(w.saved() + s.saved(), w.skipped() + s.skipped());
            }
        };
        String message = String.format("신규: %d건, 스킵: %d건", result.saved(), result.skipped());
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, message));
    }
}
