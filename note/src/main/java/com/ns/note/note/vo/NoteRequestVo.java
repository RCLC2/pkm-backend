package com.ns.note.note.vo;

import com.ns.note.note.entity.ParaCategory;

// 생성,수정 시 사용
public record NoteRequestVo(
        String workspaceId,
        String title,
        String description,
        String contents,
        ParaCategory paraCategory
) {
    public static NoteRequestVo of(
            String workspaceId,
            String title,
            String description,
            String contents,
            ParaCategory paraCategory
    )
    {
        return new NoteRequestVo(
                workspaceId,
                title,
                description,
                contents,
                paraCategory);
    }
}
