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
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Value("${jwt.refresh-exp-days}")
    private long refreshExpDays;


    @GetMapping("/google/callback")
    public ResponseEntity<?> googleLogin(@RequestParam String code) {
        AuthVo vo = userService.loginWithGoogle(GoogleLoginVo.of(code));

        HttpHeaders headers = new HttpHeaders();
        headers.add("accessToken",vo.accessToken());
        headers.add("refreshToken",vo.refreshToken());
        headers.add("Access-Control-Expose-Headers", "accessToken, refreshToken");

        Map<String, String> body = Map.of("userId", vo.userId());

        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<GlobalResponseHandler<AuthResponseDto>> refresh(
            @RequestHeader("refreshToken") String refreshToken,
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

        return GlobalResponseHandler.success(ResponseStatus.AUTH_LOGOUT_SUCCESS, null);
    }
}
