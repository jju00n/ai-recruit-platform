package com.example.airecruit.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class SignUpDto {

    private SignUpDto() {}

    @Data
    public static class SignUpReq {
        @NotBlank(message = "이름을 입력해주세요.")
        @Size(min = 2, max = 50, message = "이름은 2~50자여야 합니다.")
        private String userNm;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,20}$", message = "비밀번호는 영문과 숫자를 포함한 8자 이상이어야 합니다.")
        private String userPw;

        @NotBlank(message = "전화번호를 입력해주세요.")
        @Pattern(regexp = "^010-?\\d{4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
        private String phone;

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String userId;
    }
}
