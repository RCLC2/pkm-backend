package com.ns.note.note.dto.response;

import com.ns.note.note.entity.ParaCategory;
import com.ns.note.note.vo.NoteResponseVo;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
public class NoteResponseDto {

    private final String id;
    private final String workspaceId;
    private final String title;
    private final String description;
    private final String contents;
    private final ParaCategory paraCategory;
    private final Instant createdAt;
    private final Instant updatedAt;

    private NoteResponseDto(String id, String workspaceId, String title, String description, String contents, ParaCategory paraCategory,
                            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.title = title;
        this.description = description;
        this.contents = contents;
        this.paraCategory = paraCategory;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // VO → ResponseDto 변환
    public static NoteResponseDto from(NoteResponseVo vo) {
        return new NoteResponseDto(
                vo.id(),
                vo.workspaceId(),
                vo.title(),
                vo.description(),
                vo.contents(),
                vo.paraCategory(),
                vo.createdAt(),
                vo.updatedAt()
        );
    }
}

