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

    PERMISSION_OWNER_REGISTER_SUCCESS(HttpStatus.OK,"노트의 OWNER 설정이 성공적으로 처리되었습니다."),
    PERMISSION_GRANT_SUCCESS(HttpStatus.OK,"노트의 권한 부여가 성공적으로 처리되었습니다."),
    PERMISSION_REVOKE_SUCCESS(HttpStatus.OK,"노트의 권한 삭제가 성공적으로 처리되었습니다."),
    AUTH_TOKEN_VALID(HttpStatus.OK, "토큰이 유효합니다."),

    YORKIE_TOKEN_ISSUE(HttpStatus.OK,"요르키 단명 토큰을 성공적으로 발급하였습니다."),

    PERMISSION_ME_OK(HttpStatus.OK,"해당 노트에대한 사용자의 권한을 성공적으로 조회하였습니다." );

    private final int statusCode;
    private final String message;

    ResponseStatus(HttpStatus status, String message) {
        this.statusCode = status.value();
        this.message = message;
    }
}
