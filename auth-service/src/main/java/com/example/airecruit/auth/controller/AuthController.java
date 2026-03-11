package com.example.airecruit.auth.controller;

import com.example.airecruit.auth.dto.LoginDto;
import com.example.airecruit.auth.jwt.JwtAuthenticationFilter;
import com.example.airecruit.auth.service.AuthService;
import com.example.airecruit.auth.service.LogoutService;
import com.example.airecruit.common.dto.ResultData;
import com.example.airecruit.common.dto.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LogoutService logoutService;

    @PostMapping("/login")
    public ResponseEntity<ResultData<LoginDto.Response>> login(@RequestBody LoginDto.Request request) {
        LoginDto.Response response = authService.login(request);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResultData<?>> logout(@RequestHeader("Authorization") String bearerToken) {
        String accessToken = bearerToken.substring(JwtAuthenticationFilter.BEARER_PREFIX.length());
        logoutService.logout(accessToken);
        return ResponseEntity.ok(ResultData.of(Status.LOGOUT));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ResultData<LoginDto.Response>> reissue(@RequestBody LoginDto.ReissueRequestDto dto) {
        LoginDto.Response response = authService.reissue(dto);
        return ResponseEntity.ok(ResultData.of(Status.SUCCESS, response));
    }
}