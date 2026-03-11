package com.example.airecruit.job.crawler;

import com.example.airecruit.job.crawler.site.SaraminCrawler;
import com.example.airecruit.job.crawler.site.WantedCrawler;
import com.example.airecruit.job.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobCrawlService {

    private final WantedCrawler wantedCrawler;
    private final SaraminCrawler saraminCrawler;
    private final JobPostingRepository jobPostingRepository;
    private final JobPostingSaver jobPostingSaver;

    public CrawlResult crawlWanted() {
        log.info("[JobCrawlService] 원티드 크롤링 시작");
        List<CrawledJobData> dataList = wantedCrawler.crawl();
        return saveAll(dataList);
    }

    public CrawlResult crawlSaramin() {
        log.info("[JobCrawlService] 사람인 크롤링 시작");
        List<CrawledJobData> dataList = saraminCrawler.crawl();
        return saveAll(dataList);
    }

    /**
     * 공고 목록을 1건씩 별도 트랜잭션(REQUIRES_NEW)으로 저장.
     * 한 건 실패해도 나머지 건은 계속 저장된다.
     */
    private CrawlResult saveAll(List<CrawledJobData> dataList) {
        int saved = 0;
        int skipped = 0;

        for (CrawledJobData data : dataList) {
            if (data.getSourceUrl() == null ||
                    jobPostingRepository.existsBySourceUrl(data.getSourceUrl())) {
                skipped++;
                continue;
            }

            try {
                jobPostingSaver.saveOne(data);
                saved++;
            } catch (Exception e) {
                log.warn("[JobCrawlService] 저장 실패 - url={}, error={}",
                        data.getSourceUrl(), e.getMessage());
                skipped++;
            }
        }

        log.info("[JobCrawlService] 저장 완료 - 신규: {}건, 스킵: {}건", saved, skipped);
        return new CrawlResult(saved, skipped);
    }

    public record CrawlResult(int saved, int skipped) {}
}
