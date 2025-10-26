package com.ns.note.note.dto.request;

import com.ns.note.note.entity.ParaCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoteUpdateRequestDto {
    private String workspaceId;
    private String title;
    private String description;
    private String contents;
    private ParaCategory paraCategory;
}
