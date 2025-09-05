package com.ns.note.note.vo;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

// 노트 정보 반환 Vo
public record NoteResponseVo(
        String id,
        String title,
        String description,
        String contents,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoteResponseVo of(
            String id,
            String title,
            String description,
            String contents,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ){
        return new NoteResponseVo(
                id,
                title,
                description,
                contents,
                createdAt,
                updatedAt);
    }
}
