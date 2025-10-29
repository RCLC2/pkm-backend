package com.ns.user.user.controller;

import com.ns.user.jwt.CurrentUser;
import com.ns.user.response.GlobalResponseHandler;
import com.ns.user.response.ResponseStatus;
import com.ns.user.user.dto.request.RefreshTokenRequestDto;
import com.ns.user.user.dto.response.AuthResponseDto;
import com.ns.user.user.service.UserService;
import com.ns.user.user.vo.AuthVo;
import com.ns.user.user.vo.GoogleLoginVo;
import com.ns.user.user.vo.RefreshTokenVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Value("${jwt.refresh-exp-days}")
    private long refreshExpDays;

    @Value("${frontend.redirect.url}")
    private String frontEndRedirectUrl;

    @GetMapping("/google/callback")
    public ResponseEntity<?> googleLogin(@RequestParam String code) {
        AuthVo vo = userService.loginWithGoogle(GoogleLoginVo.of(code));

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", vo.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("None")
                .maxAge(Duration.ofDays(refreshExpDays))
                .build();

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", vo.accessToken())
                .httpOnly(false)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofMinutes(100))
                .build();

        HttpHeaders headers = new HttpHeaders();
    
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
        headers.setLocation(URI.create(frontEndRedirectUrl));

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
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
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        return GlobalResponseHandler.successWithCookie(
                ResponseStatus.AUTH_LOGOUT_SUCCESS,
                deleteCookie
        );
    }
}
