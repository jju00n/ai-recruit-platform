package com.example.airecruit.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class LoginDto {

    @Getter
    @Setter
    public static class Request {
        private String userId;
        private String userPw;
    }

    @Getter
    @AllArgsConstructor
    public static class Response {
        private String accessToken;
        private String refreshToken;
    }

    @Getter
    @Setter
    public static class ReissueRequestDto {
        private String refreshToken;
    }
}