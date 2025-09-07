package com.ns.user.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@RequiredArgsConstructor
@Getter
public enum ExceptionStatus {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),

    AUTH_INVALID_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 인증 제공자입니다."),

    AUTH_GOOGLE_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 Google 토큰입니다."),
    AUTH_GOOGLE_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 Google 토큰입니다."),

    AUTH_GOOGLE_CODE_EXCHANGE_FAILED(HttpStatus.BAD_GATEWAY, "Google 인가 코드 교환에 실패했습니다."),
    AUTH_GOOGLE_ID_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Google ID Token이 유효하지 않습니다."),
    AUTH_GOOGLE_ID_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Google ID Token이 만료되었습니다."),
    AUTH_GOOGLE_ID_TOKEN_PARSE_ERROR(HttpStatus.BAD_REQUEST, "Google ID Token 파싱 중 오류가 발생했습니다."),
    AUTH_GOOGLE_JWKS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "구글 공개키 서버와 통신할 수 없습니다."),

    AUTH_JWT_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 JWT입니다."),
    AUTH_JWT_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 JWT입니다."),


    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "리프레시 토큰을 찾을 수 없습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 일치하지 않습니다."),

    GENERAL_BAD_REQUEST(HttpStatus.BAD_REQUEST, "서버에 잘못된 요청입니다."),
    GENERAL_REQUEST_INVALID_PARAMS(HttpStatus.BAD_REQUEST, "서버에 잘못된 요청입니다."),
    SQL_FILE_LOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"SQL 파일 로딩 실패."),
    GENERAL_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 알 수 없는 오류가 발생했습니다"),
    GENERAL_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서버가 작동하지 않고 있습니다."),
    GENERAL_GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "서버에서 타임아웃이 발생했습니다");

    private final int statusCode;
    private final String message;
    private final String error;

    // 생성자도 열거형 상수 뒤에 위치
    ExceptionStatus(HttpStatus status, String message) {
        this.statusCode = status.value();
        this.message = message;
        this.error = status.getReasonPhrase();
    }
}
