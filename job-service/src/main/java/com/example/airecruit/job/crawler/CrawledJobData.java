package com.example.airecruit.job.crawler;

import com.example.airecruit.job.domain.enums.JobSource;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CrawledJobData {

    private String companyName;
    private String companyIndustry;
    private String companyLocation;

    private String title;
    private String description;
    private String responsibilities;
    private String requirements;
    private String preferredQualifications;
    private String benefits;
    private String hiringProcess;
    private String location;
    private String employmentType;  // 사이트 원문 → JobCrawlService에서 enum 매핑
    private String experienceLevel; // 사이트 원문 → JobCrawlService에서 enum 매핑
    private String jobCategory;     // 사이트 원문 → JobCrawlService에서 enum 매핑
    private String skills;          // 콤마 구분 문자열
    private String deadline;        // "yyyy-MM-dd" 또는 null

    private String sourceUrl;       // 원본 공고 URL (중복 방지 key)
    private JobSource source;
}
