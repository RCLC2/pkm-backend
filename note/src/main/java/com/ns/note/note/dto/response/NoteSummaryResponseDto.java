package com.ns.note.note.dto.response;

import com.ns.note.note.vo.NoteResponseVo;
import lombok.Getter;

import java.time.Instant;

@Getter
public class NoteSummaryResponseDto {

    private final String id;
    private final String title;
    private final String description;
    private final Instant createdAt;
    private final Instant updatedAt;

    private NoteSummaryResponseDto(String id, String title, String description, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // VO → ResponseDto 변환
    public static NoteSummaryResponseDto from(NoteResponseVo vo) {
        return new NoteSummaryResponseDto(
                vo.id(),
                vo.title(),
                vo.description(),
                vo.createdAt(),
                vo.updatedAt()
        );
    }
}

