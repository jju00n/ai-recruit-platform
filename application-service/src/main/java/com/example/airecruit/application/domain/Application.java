package com.example.airecruit.application.domain;

import com.example.airecruit.application.domain.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "jobPostingId"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long jobPostingId;

    @Column(nullable = false, length = 200)
    private String jobTitle;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    @Column(nullable = false)
    private Long resumeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void withdraw() {
        this.status = ApplicationStatus.WITHDRAWN;
    }
}
