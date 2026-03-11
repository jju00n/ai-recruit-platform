package com.example.airecruit.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AppleJwtUtils {

    private static final String APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys";

    public List<Map<String, String>> getApplePublicKeys() {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, List<Map<String, String>>> response = restTemplate.getForObject(APPLE_PUBLIC_KEYS_URL, Map.class);
        return response.get("keys");
    }

    public Claims getClaims(String identityToken) {
        try {
            List<Map<String, String>> publicKeys = getApplePublicKeys();
            String headerOfIdentityToken = identityToken.substring(0, identityToken.indexOf("."));
            Map<String, String> header = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    new String(Base64.getUrlDecoder().decode(headerOfIdentityToken), "UTF-8"), Map.class);

            Optional<Map<String, String>> matchedKey = publicKeys.stream()
                    .filter(key -> key.get("kid").equals(header.get("kid")) && key.get("alg").equals(header.get("alg")))
                    .findFirst();

            if (matchedKey.isEmpty()) {
                throw new IllegalArgumentException("일치하는 Apple 공개키를 찾을 수 없습니다.");
            }

            PublicKey publicKey = generatePublicKey(matchedKey.get());
            return Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(identityToken).getBody();
        } catch (Exception e) {
            throw new RuntimeException("Apple identityToken 검증 실패", e);
        }
    }

    private PublicKey generatePublicKey(Map<String, String> keyData) throws Exception {
        byte[] nBytes = Base64.getUrlDecoder().decode(keyData.get("n"));
        byte[] eBytes = Base64.getUrlDecoder().decode(keyData.get("e"));
        BigInteger n = new BigInteger(1, nBytes);
        BigInteger e = new BigInteger(1, eBytes);
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(n, e);
        KeyFactory keyFactory = KeyFactory.getInstance(keyData.get("kty"));
        return keyFactory.generatePublic(publicKeySpec);
    }
}