package com.ns.note.note.vo;

// 생성,수정 시 사용
public record NoteRequestVo(
        String title,
        String description,
        String contents
) {
    public static NoteRequestVo of(
            String title,
            String description,
            String contents
    )
    {
        return new NoteRequestVo(
                title,
                description,
                contents);
    }
}
