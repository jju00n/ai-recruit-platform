package com.example.airecruit.job.crawler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CrawlerScheduler {

    private final JobCrawlService jobCrawlService;

    /**
     * 매일 새벽 3시 자동 크롤링
     * 원티드 → 사람인 순서로 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void crawlAll() {
        log.info("[CrawlerScheduler] 자동 크롤링 시작");

        JobCrawlService.CrawlResult wanted = jobCrawlService.crawlWanted();
        log.info("[CrawlerScheduler] 원티드 - 신규: {}건, 스킵: {}건", wanted.saved(), wanted.skipped());

        JobCrawlService.CrawlResult saramin = jobCrawlService.crawlSaramin();
        log.info("[CrawlerScheduler] 사람인 - 신규: {}건, 스킵: {}건", saramin.saved(), saramin.skipped());

        log.info("[CrawlerScheduler] 자동 크롤링 완료 - 총 신규: {}건",
                wanted.saved() + saramin.saved());
    }
}
