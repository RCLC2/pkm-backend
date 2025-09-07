package com.ns.user.user;

import com.ns.user.exception.ExceptionStatus;
import com.ns.user.exception.ServiceException;
import com.ns.user.jwt.JwtTokenProvider;
import com.ns.user.oauth.GoogleIdTokenVerifier;
import com.ns.user.oauth.GoogleOAuthClient;
import com.ns.user.user.entity.UserEntity;
import com.ns.user.user.repository.UserRepository;
import com.ns.user.user.service.UserService;
import com.ns.user.user.vo.AuthVo;
import com.ns.user.user.vo.GoogleLoginVo;
import com.ns.user.user.vo.RefreshTokenVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;


class UserServiceTest {
    @Mock
    private GoogleOAuthClient googleOAuthClient;
    @Mock private GoogleIdTokenVerifier googleIdTokenVerifier;
    @Mock private UserRepository userRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("구글 로그인 성공 시 새로운 유저 저장 + 토큰 발급")
    void loginWithGoogle_success_newUser() {
        // given
        String code = "dummy-code";
        GoogleLoginVo loginVo = GoogleLoginVo.of(code);

        GoogleOAuthClient.GoogleTokenResponse mockResponse = new GoogleOAuthClient.GoogleTokenResponse(
                "dummy-access-token", // access_token
                "dummy-id-token",     // id_token
                null,                 // refresh_token (테스트에선 null)
                "openid email profile",
                "Bearer",
                3600L
        );

        when(googleOAuthClient.getTokens(code)).thenReturn(mockResponse);

        when(googleIdTokenVerifier.verify("dummy-id-token"))
                .thenReturn(new GoogleIdTokenVerifier.GoogleUserInfo("sub-123", "test@email.com", "tester"));

        when(userRepository.findById("sub-123")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any()))
                .thenReturn("access.jwt.token");
        when(jwtTokenProvider.generateRefreshToken(any()))
                .thenReturn("refresh.jwt.token");

        // when
        AuthVo result = userService.loginWithGoogle(loginVo);

        // then
        assertThat(result.accessToken()).isEqualTo("access.jwt.token");
        assertThat(result.refreshToken()).isEqualTo("refresh.jwt.token");

        verify(userRepository, times(1)).save(any(UserEntity.class));
        verify(valueOperations).set(startsWith("refresh:"), eq("refresh.jwt.token"), any());
    }


    @Test
    @DisplayName("RefreshToken 유효 → AccessToken 재발급 성공")
    void refreshAccessToken_success() {
        // given
        String userId = "sub-123";
        String refreshToken = "refresh.jwt.token";
        RefreshTokenVo refreshVo = RefreshTokenVo.of(userId, refreshToken);

        when(redisTemplate.opsForValue().get("refresh:" + userId))
                .thenReturn(refreshToken);
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getEmail(refreshToken)).thenReturn("test@email.com");
        when(jwtTokenProvider.getName(refreshToken)).thenReturn("tester");
        when(jwtTokenProvider.getRole(refreshToken)).thenReturn("USER");
        when(jwtTokenProvider.generateAccessToken(userId, "test@email.com", "tester", "USER"))
                .thenReturn("new.access.jwt");

        // when
        AuthVo result = userService.refreshAccessToken(refreshVo);

        // then
        assertThat(result.accessToken()).isEqualTo("new.access.jwt");
        assertThat(result.refreshToken()).isNull();
    }

    @Test
    @DisplayName("RefreshToken 불일치 → 예외 발생")
    void refreshAccessToken_mismatch() {
        // given
        String userId = "sub-123";
        RefreshTokenVo refreshVo = RefreshTokenVo.of(userId, "wrong-token");

        when(redisTemplate.opsForValue().get("refresh:" + userId))
                .thenReturn("other-token");

        // when / then
        assertThatThrownBy(() -> userService.refreshAccessToken(refreshVo))
                .isInstanceOf(ServiceException.class)
                .hasMessage(ExceptionStatus.REFRESH_TOKEN_MISMATCH.getMessage());
    }

    @Test
    @DisplayName("Redis에 RefreshToken 없음 → 예외 발생")
    void refreshAccessToken_notFound() {
        // given
        String userId = "sub-123";
        RefreshTokenVo refreshVo = RefreshTokenVo.of(userId, "some-token");

        when(redisTemplate.opsForValue().get("refresh:" + userId))
                .thenReturn(null);

        // when / then
        assertThatThrownBy(() -> userService.refreshAccessToken(refreshVo))
                .isInstanceOf(ServiceException.class)
                .hasMessage(ExceptionStatus.REFRESH_TOKEN_NOT_FOUND.getMessage());
    }


    @Test
    @DisplayName("로그아웃 시 Redis RefreshToken 삭제")
    void logout_success() {
        // given
        String userId = "sub-123";

        // when
        userService.logout(userId);

        // then
        verify(redisTemplate, times(1)).delete("refresh:" + userId);
    }
}
