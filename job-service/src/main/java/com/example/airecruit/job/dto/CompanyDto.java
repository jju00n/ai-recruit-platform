package com.example.airecruit.job.dto;

import com.example.airecruit.job.domain.Company;
import com.example.airecruit.job.domain.enums.CompanySize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class CompanyDto {

    private CompanyDto() {}

    @Getter
    public static class CreateReq {

        @NotBlank(message = "회사명을 입력해주세요.")
        @Size(max = 100, message = "회사명은 100자 이하여야 합니다.")
        private String name;

        @Size(max = 100, message = "업종은 100자 이하여야 합니다.")
        private String industry;

        private CompanySize size;

        @Size(max = 100, message = "소재지는 100자 이하여야 합니다.")
        private String location;

        private String description;

        @Size(max = 255, message = "웹사이트 URL은 255자 이하여야 합니다.")
        private String website;
    }

    @Getter
    @Builder
    public static class Response {

        private Long id;
        private String name;
        private String industry;
        private CompanySize size;
        private String location;
        private String description;
        private String website;
        private LocalDateTime createdAt;

        public static Response from(Company company) {
            return Response.builder()
                    .id(company.getId())
                    .name(company.getName())
                    .industry(company.getIndustry())
                    .size(company.getSize())
                    .location(company.getLocation())
                    .description(company.getDescription())
                    .website(company.getWebsite())
                    .createdAt(company.getCreatedAt())
                    .build();
        }
    }
}
