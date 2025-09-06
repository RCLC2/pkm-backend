package com.ns.note.note.dto.response;

import com.ns.note.note.vo.NoteResponseVo;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
public class NoteResponseDto {

    private final String id;
    private final String title;
    private final String description;
    private final String contents;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private NoteResponseDto(String id, String title, String description, String contents,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.contents = contents;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // VO → ResponseDto 변환
    public static NoteResponseDto from(NoteResponseVo vo) {
        return new NoteResponseDto(
                vo.id(),
                vo.title(),
                vo.description(),
                vo.contents(),
                vo.createdAt(),
                vo.updatedAt()
        );
    }
}

