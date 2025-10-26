package com.ns.note.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@RequiredArgsConstructor
@Getter
public enum ExceptionStatus {

    NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 노트의 정보를 찾을 수 없습니다"),

    GENERAL_BAD_REQUEST(HttpStatus.BAD_REQUEST, "서버에 잘못된 요청입니다."),
    GENERAL_REQUEST_INVALID_PARAMS(HttpStatus.BAD_REQUEST, "서버에 잘못된 요청입니다."),
    SQL_FILE_LOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"SQL 파일 로딩 실패."),
    GENERAL_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 알 수 없는 오류가 발생했습니다"),
    GENERAL_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서버가 작동하지 않고 있습니다."),
    GENERAL_GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "서버에서 타임아웃이 발생했습니다"),

    NOTE_SERVICE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "노트에 대한 권한이 없습니다."),
    OWNER_PERMISSION_REGISTER_FAILED(HttpStatus.BAD_REQUEST,"노트 소유자 권한 등록 중 오류가 발생했습니다."),
    NOTE_SERVICE_NOT_AUTHENTICATION_ROLE(HttpStatus.BAD_REQUEST ,"접근 권한(ROLE)이 적절하지않은 사용자입니다."),
    USER_SERVICE_ACCESS_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "USER 서비스 도메인에 접근할 수 없습니다."),
    USER_SERVICE_INVALID_ROLE(HttpStatus.BAD_REQUEST, "USER 서비스에서 잘못된 역할이 전달되었습니다."),
    NOTE_PARA_MAPPING_INCOMPLETE(HttpStatus.BAD_REQUEST, "모든 노트가 PARA 카테고리로 매핑되어야 합니다." );

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
