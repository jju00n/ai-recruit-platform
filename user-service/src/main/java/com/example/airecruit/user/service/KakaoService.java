package com.example.airecruit.user.service;

import com.example.airecruit.user.dto.KakaoTokenResponse;
import com.example.airecruit.user.dto.KakaoUserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoService {

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.web-redirect-uri}")
    private String redirectUri;

    @Value("${kakao.token-uri}")
    private String tokenUri;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.user-info-uri}")
    private String userInfoUri;

    private final RestTemplate restTemplate = new RestTemplate();

    public KakaoUserInfoResponse getUserInfoWithToken(String accessToken) {
        return getUserInfo(accessToken);
    }

    public KakaoUserInfoResponse getUserInfoWithCode(String code, String codeVerifier) {
        String accessToken = getAccessToken(code, codeVerifier);
        return getUserInfo(accessToken);
    }

    private String getAccessToken(String code, String codeVerifier) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        params.add("client_secret", clientSecret);
        params.add("code_verifier", codeVerifier);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        try {
            ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(tokenUri, request, KakaoTokenResponse.class);
            if (response.getBody() == null) throw new RuntimeException("카카오 토큰 응답이 비어있습니다.");
            return response.getBody().getAccessToken();
        } catch (HttpClientErrorException e) {
            log.error("카카오 토큰 발급 실패: {}", e.getResponseBodyAsString());
            throw new RuntimeException("카카오 토큰 발급에 실패했습니다.", e);
        }
    }

    private KakaoUserInfoResponse getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<KakaoUserInfoResponse> response = restTemplate.postForEntity(userInfoUri, request, KakaoUserInfoResponse.class);
            if (response.getBody() == null) throw new RuntimeException("카카오 사용자 정보 응답이 비어있습니다.");
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("카카오 사용자 정보 조회 실패: {}", e.getResponseBodyAsString());
            throw new RuntimeException("카카오 사용자 정보 조회에 실패했습니다.", e);
        }
    }
}
