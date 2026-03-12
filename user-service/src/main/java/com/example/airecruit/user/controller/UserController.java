package com.example.airecruit.user.controller;

import com.example.airecruit.common.dto.ResultData;
import com.example.airecruit.common.dto.Status;
import com.example.airecruit.user.dto.LoginDto;
import com.example.airecruit.user.dto.SignUpDto;
import com.example.airecruit.user.dto.UserDto;
import com.example.airecruit.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<ResultData<?>> signUp(@Valid @RequestBody SignUpDto.SignUpReq dto) {
        return userService.signUp(dto);
    }

    @PostMapping("/email/send")
    public ResponseEntity<ResultData<?>> sendEmailVerification(@RequestParam String email) {
        userService.sendEmailVerification(email);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS));
    }

    @PostMapping("/email/verify")
    public ResponseEntity<ResultData<?>> verifyEmail(@RequestParam String email, @RequestParam String code) {
        boolean result = userService.verifyEmail(email, code);
        if (result) {
            return ResponseEntity.ok(ResultData.of(Status.SUCCESS));
        }
        return ResponseEntity.badRequest().body(ResultData.of(Status.AUTHENTICATION_FAIL));
    }

    @GetMapping("/me")
    public ResponseEntity<ResultData<UserDto>> getUserInfo(@RequestHeader("X-User-Id") Long userId) {
        UserDto userDto = userService.getUserInfo(userId);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, userDto));
    }

    @PostMapping("/login/kakao")
    public ResponseEntity<ResultData<LoginDto.Response>> loginWithKakao(@RequestBody LoginDto.KakaoLoginRequestDto dto) {
        LoginDto.Response response = userService.loginWithKakao(dto);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }

    @PostMapping("/login/apple")
    public ResponseEntity<ResultData<LoginDto.Response>> loginWithApple(@RequestBody LoginDto.AppleLoginRequestDto dto) {
        LoginDto.Response response = userService.loginWithApple(dto);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }

    // 내부 서비스 간 통신용 (notification-service → user-service 직접 호출)
    @GetMapping("/internal/{idx}")
    public ResponseEntity<ResultData<String>> getInternalUserEmail(@PathVariable Long idx) {
        String email = userService.getUserEmail(idx);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, "이메일 조회 성공", email));
    }
}
