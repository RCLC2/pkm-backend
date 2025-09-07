package com.ns.user.jwt;

import com.ns.user.exception.AuthException;
import com.ns.user.exception.ExceptionStatus;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    private final String secretKey;
    private final long accessExpMinutes;
    private final long refreshExpDays;

    private SecretKey key;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-exp-minutes}") long accessExpMinutes,
            @Value("${jwt.refresh-exp-days}") long refreshExpDays
    ) {
        this.secretKey = secretKey;
        this.accessExpMinutes = accessExpMinutes;
        this.refreshExpDays = refreshExpDays;
    }

    @PostConstruct
    private void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // Access Token 생성
    public String generateAccessToken(String userId, String email, String name, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpMinutes * 60 * 1000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("name", name);
        claims.put("role", role);
        claims.put("type", "access");

        return Jwts.builder()
                .setSubject(userId) // Google sub
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, Jwts.SIG.HS256) // HMAC-SHA256 서명
                .compact();
    }

    //  Refresh Token 생성
    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpDays * 24 * 60 * 60 * 1000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        return Jwts.builder()
                .setSubject(userId) // Google sub
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new AuthException(ExceptionStatus.AUTH_JWT_EXPIRED);
        } catch (IllegalArgumentException | JwtException e) {
            throw new AuthException(ExceptionStatus.AUTH_JWT_INVALID);
        } catch (SecurityException e) {
            throw new AuthException(ExceptionStatus.UNAUTHORIZED); // 서명 오류 → 인증 불가
        }
    }

    // Claims 추출
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUserId(String token) {
        return getClaims(token).getSubject();
    }

    public String getEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    public String getName(String token) {
        return getClaims(token).get("name", String.class);
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getClaims(token).get("type", String.class));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getClaims(token).get("type", String.class));
    }
}
