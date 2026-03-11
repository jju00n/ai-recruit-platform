package com.example.airecruit.job.domain;

import com.example.airecruit.job.domain.enums.CompanySize;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("회사 고유 ID")
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    @Comment("회사명 (크롤링 중복 방지 유니크)")
    private String name;

    @Column(length = 100)
    @Comment("업종 (예: IT, 금융, 제조)")
    private String industry;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Comment("회사 규모 (STARTUP: ~50명, SME: ~300명, LARGE: 300명+)")
    private CompanySize size;

    @Column(length = 100)
    @Comment("본사 소재지")
    private String location;

    @Lob
    @Comment("회사 소개")
    private String description;

    @Column(length = 255)
    @Comment("웹사이트 URL")
    private String website;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    @Comment("생성일시")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    @Comment("수정일시")
    private LocalDateTime updatedAt;
}
