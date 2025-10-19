package com.ns.note.note.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoteCreateRequestDto {
    private String workspaceId;
    private String title;
    private String description;
    private String contents;
}
