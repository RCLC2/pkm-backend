package com.ns.user.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@RequiredArgsConstructor
@Getter
public enum ResponseStatus {
    USER_REGISTER_SUCCESS(HttpStatus.CREATED, "회원가입이 성공적으로 완료되었습니다."),
    USER_INFO_SUCCESS(HttpStatus.OK, "사용자 정보 조회에 성공했습니다."),

    AUTH_LOGIN_SUCCESS(HttpStatus.OK, "로그인에 성공했습니다."),
    AUTH_LOGOUT_SUCCESS(HttpStatus.OK, "로그아웃이 성공적으로 처리되었습니다."),
    AUTH_REFRESH_SUCCESS(HttpStatus.OK, "토큰이 성공적으로 재발급되었습니다."),
    AUTH_TOKEN_VALID(HttpStatus.OK, "토큰이 유효합니다.");

    private final int statusCode;
    private final String message;

    ResponseStatus(HttpStatus status, String message) {
        this.statusCode = status.value();
        this.message = message;
    }
}
