package com.example.airecruit.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "social_accounts")
@EntityListeners(AuditingEntityListener.class)
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String provider;

    @Column(name = "social_id", unique = true)
    private String socialId;

    @CreatedDate
    @Column(name = "linked_at", updatable = false)
    private LocalDateTime linkedAt;

    @Builder
    public SocialAccount(User user, String provider, String socialId) {
        this.user = user;
        this.provider = provider;
        this.socialId = socialId;
    }
}
