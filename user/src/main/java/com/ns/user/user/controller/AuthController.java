package com.ns.user.user.controller;

import com.ns.user.jwt.CurrentUser;
import com.ns.user.response.GlobalResponseHandler;
import com.ns.user.response.ResponseStatus;
import com.ns.user.user.dto.request.GoogleLoginRequestDto;
import com.ns.user.user.dto.request.RefreshTokenRequestDto;
import com.ns.user.user.dto.response.AuthResponseDto;
import com.ns.user.user.service.UserService;
import com.ns.user.user.vo.AuthVo;
import com.ns.user.user.vo.GoogleLoginVo;
import com.ns.user.user.vo.RefreshTokenVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Value("${jwt.refresh-exp-days}")
    private long refreshExpDays;

    @PostMapping("/google/callback")
    public ResponseEntity<GlobalResponseHandler<AuthResponseDto>> googleLogin(
            @RequestBody GoogleLoginRequestDto request
    ) {
        AuthVo vo = userService.loginWithGoogle(GoogleLoginVo.of(request.getCode()));

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", vo.refreshToken())
                .httpOnly(true)
                .secure(false) // true 변경 필요
                .path("/")
                .sameSite("Lax") // Secure 변경 필ㅇㅅ
                .maxAge(Duration.ofDays(refreshExpDays))
                .build();

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", vo.accessToken())
                .httpOnly(false)
                .secure(false) // true 변경
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(100))
                .build();

        AuthResponseDto responseDto = new AuthResponseDto(vo.accessToken());

        return GlobalResponseHandler.successWithCookie(
                ResponseStatus.AUTH_LOGIN_SUCCESS,
                responseDto,
                refreshCookie,
                accessCookie
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<GlobalResponseHandler<AuthResponseDto>> refresh(
            @CookieValue("refreshToken") String refreshToken,
            @RequestBody RefreshTokenRequestDto request
    ) {

        AuthVo vo = userService.refreshAccessToken(RefreshTokenVo.of(
                request.getUserId(),
                refreshToken));

        AuthResponseDto responseDto = new AuthResponseDto(vo.accessToken());

        return GlobalResponseHandler.success(
                ResponseStatus.AUTH_REFRESH_SUCCESS,
                responseDto
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<GlobalResponseHandler<Void>> logout(
            @AuthenticationPrincipal CurrentUser currentUser) {
        userService.logout(currentUser.id());

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false) // true 변경
                .path("/")
                .maxAge(0)
                .build();

        return GlobalResponseHandler.successWithCookie(
                ResponseStatus.AUTH_LOGOUT_SUCCESS,
                deleteCookie
        );
    }
}
