package com.ns.note.note.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResponseHandlerDto {
    private Integer statusCode;
    private String message;
    private PermissionMeResponseDto data;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionMeResponseDto {
        private String noteId;
        private String userId;
        private String role; // OWNER | WRITER | READER
    }
}
