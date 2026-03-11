package com.example.airecruit.user.dto;

import com.example.airecruit.user.domain.SocialAccount;
import com.example.airecruit.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class UserDto {

    private final String userId;
    private final String userName;
    private final String phone;
    private final String provider;
    private final boolean isKakaoLinked;
    private final LocalDateTime kakaoLinkedAt;
    private final boolean isAppleLinked;
    private final LocalDateTime appleLinkedAt;

    @Builder
    private UserDto(String userId, String userName, String phone, String provider,
                    boolean isKakaoLinked, LocalDateTime kakaoLinkedAt,
                    boolean isAppleLinked, LocalDateTime appleLinkedAt) {
        this.userId = userId;
        this.userName = userName;
        this.phone = phone;
        this.provider = provider;
        this.isKakaoLinked = isKakaoLinked;
        this.kakaoLinkedAt = kakaoLinkedAt;
        this.isAppleLinked = isAppleLinked;
        this.appleLinkedAt = appleLinkedAt;
    }

    public static UserDto of(User user) {
        LocalDateTime kakaoTime = null;
        LocalDateTime appleTime = null;
        String determinedProvider = "unknown";

        if (user.getUserPw() != null && !user.getUserPw().isEmpty()) {
            determinedProvider = "local";
        } else {
            List<SocialAccount> accounts = user.getSocialAccounts();
            if (accounts != null && !accounts.isEmpty()) {
                determinedProvider = accounts.get(0).getProvider().toUpperCase();
            }
        }

        List<SocialAccount> accounts = user.getSocialAccounts();
        if (accounts != null) {
            for (SocialAccount account : accounts) {
                String currentProvider = account.getProvider();
                if ("KAKAO".equalsIgnoreCase(currentProvider)) {
                    kakaoTime = account.getLinkedAt();
                } else if ("APPLE".equalsIgnoreCase(currentProvider)) {
                    appleTime = account.getLinkedAt();
                }
            }
        }

        return UserDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserNm())
                .phone(user.getPhone())
                .provider(determinedProvider)
                .isKakaoLinked(kakaoTime != null)
                .kakaoLinkedAt(kakaoTime)
                .isAppleLinked(appleTime != null)
                .appleLinkedAt(appleTime)
                .build();
    }
}
