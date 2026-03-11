package com.example.airecruit.user.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret.key}")
    private String secretKey;
    private Key key;

    private final long accessTokenValidityInMilliseconds = 30 * 60 * 1000L;
    private final long refreshTokenValidityInMilliseconds = 14 * 24 * 60 * 60 * 1000L;

    @PostConstruct
    protected void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long idx, String userId, String role) {
        return createToken(idx, userId, role, accessTokenValidityInMilliseconds);
    }

    public String createRefreshToken(String userId) {
        Date now = new Date();
        Date validityDate = new Date(now.getTime() + refreshTokenValidityInMilliseconds);
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(validityDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String createToken(Long idx, String userId, String role, long validity) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("roles", List.of(role));
        claims.put("idx", idx);
        Date now = new Date();
        Date validityDate = new Date(now.getTime() + validity);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validityDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long getExpiration(String accessToken) {
        Date expirationDate = parseClaims(accessToken).getExpiration();
        return expirationDate.getTime() - new Date().getTime();
    }

    public String getUserIdFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}