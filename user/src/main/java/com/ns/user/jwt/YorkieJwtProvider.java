package com.ns.user.jwt;

import com.ns.user.exception.AuthException;
import com.ns.user.exception.ExceptionStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class YorkieJwtProvider {

    @Value("${yorkie.jwt.secret:${YORKIE_JWT_SECRET}}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // yorkie 토큰 생성
    public String generateYorkieToken(String userId,
                       String noteId,
                       String role,   // OWNER|WRITER|READER
                       String verb,   // r|rw
                       Duration ttl) {

        Date now = new Date();
        Date exp = new Date(now.getTime() + ttl.toMillis());

        return Jwts.builder()
                .setSubject(userId)
                .claim("noteId", noteId)
                .claim("role", role)
                .claim("verb", verb)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // yorkie 토큰 검증
    public Claims validateYorkieToken(String token) {
        try {
           return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new AuthException(ExceptionStatus.AUTH_YORKIE_JWT_EXPIRED);
        } catch (IllegalArgumentException | JwtException e) {
            throw new AuthException(ExceptionStatus.AUTH_YORKIE_JWT_INVALID);
        } catch (SecurityException e) {
            throw new AuthException(ExceptionStatus.UNAUTHORIZED); // 서명 오류 → 인증 불가
        }
    }

    // yorkie 토큰 검증 + 클레임 값 추출
    public YorkieClaims verifiedYorkieClaims(String token) {
        Claims c = validateYorkieToken(token);
        String userId = c.getSubject();
        String noteId = c.get("noteId", String.class);
        String role = c.get("role", String.class);
        String verb = c.get("verb", String.class);

        return new YorkieClaims(userId, noteId, role, verb);
    }

    // yorkie 토큰의 클레임 값을 담는 DTO
    @Getter
    public static class YorkieClaims {
        private final String userId;
        private final String noteId;
        private final String role;
        private final String verb;

        private YorkieClaims(String userId, String noteId, String role, String verb) {
            this.userId = userId;
            this.noteId = noteId;
            this.role = role;
            this.verb = verb;
        }
    }
}
