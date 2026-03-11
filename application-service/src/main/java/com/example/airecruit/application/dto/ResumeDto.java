package com.example.airecruit.application.dto;

import com.example.airecruit.application.domain.Resume;
import com.example.airecruit.application.domain.enums.ResumeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class ResumeDto {

    private ResumeDto() {}

    @Getter
    public static class TextCreateReq {

        @NotBlank(message = "이력서 제목을 입력해주세요.")
        @Size(max = 200, message = "이력서 제목은 200자 이하여야 합니다.")
        private String title;

        @NotBlank(message = "이력서 내용을 입력해주세요.")
        private String content;
    }

    @Getter
    public static class TextUpdateReq {

        @NotBlank(message = "이력서 제목을 입력해주세요.")
        @Size(max = 200, message = "이력서 제목은 200자 이하여야 합니다.")
        private String title;

        @NotBlank(message = "이력서 내용을 입력해주세요.")
        private String content;
    }

    @Getter
    @Builder
    public static class Response {

        private Long id;
        private Long userId;
        private String title;
        private ResumeType resumeType;
        private String content;
        private String originalFilename;
        private Boolean isDefault;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Resume resume) {
            return Response.builder()
                    .id(resume.getId())
                    .userId(resume.getUserId())
                    .title(resume.getTitle())
                    .resumeType(resume.getResumeType())
                    .content(resume.getContent())
                    .originalFilename(resume.getOriginalFilename())
                    .isDefault(resume.getIsDefault())
                    .createdAt(resume.getCreatedAt())
                    .updatedAt(resume.getUpdatedAt())
                    .build();
        }
    }
}
