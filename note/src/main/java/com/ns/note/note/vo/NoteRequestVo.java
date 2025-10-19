package com.ns.note.note.vo;

// 생성,수정 시 사용
public record NoteRequestVo(
        String workspaceId,
        String title,
        String description,
        String contents
) {
    public static NoteRequestVo of(
        String workspaceId,
            String title,
            String description,
            String contents
    )
    {
        return new NoteRequestVo(
                workspaceId,
                title,
                description,
                contents);
    }
}
