package com.ns.note.note.vo;

import com.ns.note.note.entity.ParaCategory;

import java.util.List;

public record NoteParaMappingVo(
        String workspaceId,
        List<MappingItem> mappings
) {
    public record MappingItem(
            String noteId,
            ParaCategory paraCategory
    ) {}
}
