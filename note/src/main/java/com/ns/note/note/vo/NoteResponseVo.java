package com.ns.note.note.vo;

import com.ns.note.note.entity.ParaCategory;

import java.time.Instant;

// 노트 정보 반환 Vo
public record NoteResponseVo(
        String id,
        String workspaceId,
        String title,
        String description,
        String contents,
        ParaCategory paraCategory,
        Instant createdAt,
        Instant updatedAt
) {
    public static NoteResponseVo of(
            String id,
            String workspaceId,
            String title,
            String description,
            String contents,
            ParaCategory paraCategory,
            Instant createdAt,
            Instant updatedAt
    ){
        return new NoteResponseVo(
                id,
                workspaceId,
                title,
                description,
                contents,
                paraCategory,
                createdAt,
                updatedAt);
    }
}
