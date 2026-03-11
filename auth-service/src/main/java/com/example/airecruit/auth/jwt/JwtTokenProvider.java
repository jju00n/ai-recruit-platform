package com.example.airecruit.auth.jwt;

import com.example.airecruit.auth.service.UserDetailsServiceImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret.key}")
    private String secretKey;
    private Key key;

    private final long accessTokenValidityInMilliseconds = 30 * 60 * 1000L;
    private final long refreshTokenValidityInMilliseconds = 14 * 24 * 60 * 60 * 1000L;

    private final UserDetailsServiceImpl userDetailsService;

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

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        Collection<? extends GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        UserDetails principal = userDetailsService.loadUserByUsername(claims.getSubject());
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT exception: {}", e.getMessage());
        }
        return false;
    }

    public Long getExpiration(String accessToken) {
        Date expirationDate = parseClaims(accessToken).getExpiration();
        long now = new Date().getTime();
        return expirationDate.getTime() - now;
    }

    public String getUserIdFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid token passed to parseClaims", e);
        }
    }
}
