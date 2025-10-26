package com.ns.note.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@RequiredArgsConstructor
@Getter
public enum ResponseStatus {
    NOTE_CREATE_SUCCESS(HttpStatus.OK, "새로운 노트가 성공적으로 생성되었습니다."),
    NOTE_DELETE_SUCCESS(HttpStatus.OK, "노트가 성공적으로 삭제되었습니다."),
    NOTE_UPDATE_SUCCESS(HttpStatus.OK, "노트가 성공적으로 수정되었습니다."),
    NOTE_SEARCH_SUCCESS(HttpStatus.OK, "노트가 성공적으로 조회되었습니다."),
    NOTE_PARA_MAPPING_SUCCESS(HttpStatus.OK,"파라 변환이 성공적으로 완료되었습니다.");

    private final int statusCode;
    private final String message;

    ResponseStatus(HttpStatus status, String message) {
        this.statusCode = status.value();
        this.message = message;
    }
}
