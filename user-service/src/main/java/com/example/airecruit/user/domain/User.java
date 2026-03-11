package com.example.airecruit.user.domain;

import com.example.airecruit.user.entity.PhoneEncryptConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(name = "user_nm", nullable = false, length = 50)
    private String userNm;

    @Column(name = "user_id", unique = true, nullable = false, length = 100)
    private String userId;

    @Column(nullable = true)
    private String userPw;

    @Convert(converter = PhoneEncryptConverter.class)
    @Column(name = "phone", nullable = true, length = 512)
    private String phone;

    private String role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    public void addSocialAccount(SocialAccount socialAccount) {
        this.socialAccounts.add(socialAccount);
    }
}