package com.example.airecruit.job.dto;

import com.example.airecruit.job.domain.JobPosting;
import com.example.airecruit.job.domain.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JobPostingDto {

    private JobPostingDto() {}

    @Getter
    public static class CreateReq {

        @NotNull(message = "회사 ID를 입력해주세요.")
        private Long companyId;

        @NotBlank(message = "공고 제목을 입력해주세요.")
        @Size(max = 200, message = "공고 제목은 200자 이하여야 합니다.")
        private String title;

        @NotBlank(message = "공고 내용을 입력해주세요.")
        private String description;

        private String responsibilities;
        private String requirements;
        private String preferredQualifications;
        private String benefits;
        private String hiringProcess;

        @NotNull(message = "직무 카테고리를 선택해주세요.")
        private JobCategory jobCategory;

        @NotBlank(message = "근무 지역을 입력해주세요.")
        @Size(max = 100, message = "근무 지역은 100자 이하여야 합니다.")
        private String location;

        @NotNull(message = "고용형태를 선택해주세요.")
        private EmploymentType employmentType;

        @NotNull(message = "경력 수준을 선택해주세요.")
        private ExperienceLevel experienceLevel;

        private Integer minSalary;
        private Integer maxSalary;

        @Size(max = 500, message = "기술스택은 500자 이하여야 합니다.")
        private String skills;

        private LocalDate deadline;
    }

    @Getter
    public static class UpdateReq {

        @NotBlank(message = "공고 제목을 입력해주세요.")
        @Size(max = 200, message = "공고 제목은 200자 이하여야 합니다.")
        private String title;

        @NotBlank(message = "공고 내용을 입력해주세요.")
        private String description;

        private String responsibilities;
        private String requirements;
        private String preferredQualifications;
        private String benefits;
        private String hiringProcess;

        @NotNull(message = "직무 카테고리를 선택해주세요.")
        private JobCategory jobCategory;

        @NotBlank(message = "근무 지역을 입력해주세요.")
        @Size(max = 100, message = "근무 지역은 100자 이하여야 합니다.")
        private String location;

        @NotNull(message = "고용형태를 선택해주세요.")
        private EmploymentType employmentType;

        @NotNull(message = "경력 수준을 선택해주세요.")
        private ExperienceLevel experienceLevel;

        private Integer minSalary;
        private Integer maxSalary;

        @Size(max = 500, message = "기술스택은 500자 이하여야 합니다.")
        private String skills;

        @NotNull(message = "공고 상태를 선택해주세요.")
        private JobStatus status;

        private LocalDate deadline;
    }

    @Getter
    @Builder
    public static class Response {

        private Long id;
        private CompanyDto.Response company;
        private String title;
        private String description;
        private String responsibilities;
        private String requirements;
        private String preferredQualifications;
        private String benefits;
        private String hiringProcess;
        private JobCategory jobCategory;
        private String location;
        private EmploymentType employmentType;
        private ExperienceLevel experienceLevel;
        private Integer minSalary;
        private Integer maxSalary;
        private List<String> skills;
        private JobStatus status;
        private LocalDate deadline;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(JobPosting jp) {
            return Response.builder()
                    .id(jp.getId())
                    .company(CompanyDto.Response.from(jp.getCompany()))
                    .title(jp.getTitle())
                    .description(jp.getDescription())
                    .responsibilities(jp.getResponsibilities())
                    .requirements(jp.getRequirements())
                    .preferredQualifications(jp.getPreferredQualifications())
                    .benefits(jp.getBenefits())
                    .hiringProcess(jp.getHiringProcess())
                    .jobCategory(jp.getJobCategory())
                    .location(jp.getLocation())
                    .employmentType(jp.getEmploymentType())
                    .experienceLevel(jp.getExperienceLevel())
                    .minSalary(jp.getMinSalary())
                    .maxSalary(jp.getMaxSalary())
                    .skills(parseSkills(jp.getSkills()))
                    .status(jp.getStatus())
                    .deadline(jp.getDeadline())
                    .createdAt(jp.getCreatedAt())
                    .updatedAt(jp.getUpdatedAt())
                    .build();
        }

        private static List<String> parseSkills(String skills) {
            if (skills == null || skills.isBlank()) return Collections.emptyList();
            return Arrays.stream(skills.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }

    @Getter
    @Builder
    public static class SearchResponse {

        private Long id;
        private Long companyId;
        private String companyName;
        private String title;
        private String description;
        private JobCategory jobCategory;
        private String location;
        private EmploymentType employmentType;
        private ExperienceLevel experienceLevel;
        private Integer minSalary;
        private Integer maxSalary;
        private List<String> skills;
        private LocalDate deadline;
    }
}
