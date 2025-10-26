package com.ns.note.note.dto.response;

import com.ns.note.note.entity.ParaCategory;
import com.ns.note.note.vo.NoteResponseVo;
import lombok.Getter;

import java.time.Instant;

@Getter
public class NoteSummaryResponseDto {

    private final String id;
    private final String title;
    private final String description;
    private final ParaCategory paraCategory;
    private final Instant createdAt;
    private final Instant updatedAt;

    private NoteSummaryResponseDto(String id, String title, String description, ParaCategory paraCategory, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.paraCategory = paraCategory;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // VO → ResponseDto 변환
    public static NoteSummaryResponseDto from(NoteResponseVo vo) {
        return new NoteSummaryResponseDto(
                vo.id(),
                vo.title(),
                vo.description(),
                vo.paraCategory(),
                vo.createdAt(),
                vo.updatedAt()
        );
    }
}

