package com.ns.note.note.vo;

import java.time.Instant;

// 노트 정보 반환 Vo
public record NoteResponseVo(
        String id,
        String title,
        String description,
        String contents,
        Instant createdAt,
        Instant updatedAt
) {
    public static NoteResponseVo of(
            String id,
            String title,
            String description,
            String contents,
            Instant createdAt,
            Instant updatedAt
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
