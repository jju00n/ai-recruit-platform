package com.example.airecruit.job.domain;

import com.example.airecruit.job.domain.enums.*;
import com.example.airecruit.job.domain.enums.JobSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_postings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("채용공고 고유 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @Comment("회사 ID (companies.id FK)")
    private Company company;

    @Column(nullable = false, length = 200)
    @Comment("공고 제목")
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    @Comment("공고 상세 내용 (JD)")
    private String description;

    @Lob
    @Column(columnDefinition = "TEXT")
    @Comment("주요업무")
    private String responsibilities;

    @Lob
    @Column(columnDefinition = "TEXT")
    @Comment("자격요건")
    private String requirements;

    @Lob
    @Column(columnDefinition = "TEXT")
    @Comment("우대사항")
    private String preferredQualifications;

    @Lob
    @Column(columnDefinition = "TEXT")
    @Comment("혜택 및 복지")
    private String benefits;

    @Lob
    @Column(columnDefinition = "TEXT")
    @Comment("채용 전형")
    private String hiringProcess;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Comment("직무 카테고리 (BACKEND, FRONTEND, DEVOPS 등)")
    private JobCategory jobCategory;

    @Column(nullable = false, length = 100)
    @Comment("근무 지역")
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("고용형태 (FULL_TIME, PART_TIME, CONTRACT, INTERN)")
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("경력 수준 (NEWCOMER: 신입, JUNIOR: 1-3년, SENIOR: 4-7년, LEAD: 8년+)")
    private ExperienceLevel experienceLevel;

    @Comment("최소 연봉 (만원 단위)")
    private Integer minSalary;

    @Comment("최대 연봉 (만원 단위)")
    private Integer maxSalary;

    @Column(length = 500)
    @Comment("기술스택 (콤마 구분, 예: Java,Spring,MySQL)")
    private String skills;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    @Comment("공고 상태 (DRAFT: 임시저장, OPEN: 진행중, CLOSED: 마감)")
    private JobStatus status = JobStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    @Comment("공고 출처 (MANUAL: 직접등록, SARAMIN: 사람인, WANTED: 원티드 등)")
    private JobSource source = JobSource.MANUAL;

    @Column(length = 500, unique = true)
    @Comment("크롤링 원본 URL (MANUAL이면 null, 중복 방지 유니크)")
    private String sourceUrl;

    @Comment("공고 마감일")
    private LocalDate deadline;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    @Comment("생성일시")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    @Comment("수정일시")
    private LocalDateTime updatedAt;

    public void update(String title, String description, String responsibilities,
                       String requirements, String preferredQualifications,
                       String benefits, String hiringProcess,
                       JobCategory jobCategory, String location,
                       EmploymentType employmentType, ExperienceLevel experienceLevel,
                       Integer minSalary, Integer maxSalary, String skills,
                       JobStatus status, LocalDate deadline) {
        this.title = title;
        this.description = description;
        this.responsibilities = responsibilities;
        this.requirements = requirements;
        this.preferredQualifications = preferredQualifications;
        this.benefits = benefits;
        this.hiringProcess = hiringProcess;
        this.jobCategory = jobCategory;
        this.location = location;
        this.employmentType = employmentType;
        this.experienceLevel = experienceLevel;
        this.minSalary = minSalary;
        this.maxSalary = maxSalary;
        this.skills = skills;
        this.status = status;
        this.deadline = deadline;
    }

    public void close() {
        this.status = JobStatus.CLOSED;
    }
}
