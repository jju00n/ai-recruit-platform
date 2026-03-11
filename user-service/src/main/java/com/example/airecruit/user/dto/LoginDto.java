package com.example.airecruit.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class LoginDto {

    @Getter
    @AllArgsConstructor
    public static class Response {
        private String accessToken;
        private String refreshToken;
    }

    @Getter
    @Setter
    public static class AppleLoginRequestDto {
        private String userId;
        private String userNm;
        private String identityToken;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoLoginRequestDto {
        private String code;
        private String code_verifier;
        private String accessToken;
    }
}