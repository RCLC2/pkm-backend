package com.ns.user.user.service;

import com.ns.user.exception.ExceptionStatus;
import com.ns.user.exception.ServiceException;
import com.ns.user.jwt.JwtTokenProvider;
import com.ns.user.oauth.GoogleIdTokenVerifier;
import com.ns.user.oauth.GoogleOAuthClient;
import com.ns.user.user.entity.UserEntity;
import com.ns.user.user.repository.UserRepository;
import com.ns.user.user.vo.AuthVo;
import com.ns.user.user.vo.GoogleLoginVo;
import com.ns.user.user.vo.RefreshTokenVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final GoogleOAuthClient googleOAuthClient;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final JwtTokenProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-exp-days}")
    private long refreshExpDays;

    private static final String REFRESH_PREFIX = "refresh:";

    public AuthVo loginWithGoogle(GoogleLoginVo loginVo) {
        GoogleOAuthClient.GoogleTokenResponse tokenResponse = googleOAuthClient.getTokens(loginVo.code());
        GoogleIdTokenVerifier.GoogleUserInfo userInfo = googleIdTokenVerifier.verify(tokenResponse.idToken());

        // 사용자 조회시 존재하지 않는 회원이면 회원가입
        UserEntity user = userRepository.findById(userInfo.sub())
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .id(userInfo.sub())
                        .email(userInfo.email())
                        .name(userInfo.name())
                        .role("USER")
                        .oauthProvider("GOOGLE")
                        .build()));

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail(), user.getName(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        redisTemplate.opsForValue().set(REFRESH_PREFIX + user.getId(), refreshToken, Duration.ofDays(refreshExpDays));

        return new AuthVo(accessToken, refreshToken);
    }

    public AuthVo refreshAccessToken(RefreshTokenVo refreshVo) {
        String savedToken = redisTemplate.opsForValue().get(REFRESH_PREFIX + refreshVo.userId());

        if (savedToken == null) throw new ServiceException(ExceptionStatus.REFRESH_TOKEN_NOT_FOUND);
        if (!savedToken.equals(refreshVo.refreshToken())) throw new ServiceException(ExceptionStatus.REFRESH_TOKEN_MISMATCH);
        if (!jwtProvider.validateToken(refreshVo.refreshToken()) || !jwtProvider.isRefreshToken(refreshVo.refreshToken())) {
            throw new ServiceException(ExceptionStatus.REFRESH_TOKEN_EXPIRED);
        }

        String email = jwtProvider.getEmail(refreshVo.refreshToken());
        String name = jwtProvider.getName(refreshVo.refreshToken());
        String role = jwtProvider.getRole(refreshVo.refreshToken());

        String newAccessToken = jwtProvider.generateAccessToken(refreshVo.userId(), email, name, role);
        return new AuthVo(newAccessToken, null);
    }

    public void logout(String userId) {
        redisTemplate.delete(REFRESH_PREFIX + userId);
    }
}
