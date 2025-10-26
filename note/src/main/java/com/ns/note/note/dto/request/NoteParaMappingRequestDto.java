package com.ns.note.note.dto.request;


import com.ns.note.note.entity.ParaCategory;
import com.ns.note.note.vo.NoteParaMappingVo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoteParaMappingRequestDto {
    private List<NoteParaMappingItem> mappings;
    private String workspaceId;

    @Getter
    @NoArgsConstructor
    public static class NoteParaMappingItem {
        private String noteId;
        private ParaCategory paraCategory;
    }

    public NoteParaMappingVo toVo() {
        return new NoteParaMappingVo(
                workspaceId,
                mappings.stream()
                        .map(m -> new NoteParaMappingVo.MappingItem(m.getNoteId(), m.getParaCategory()))
                        .collect(Collectors.toList())
        );
    }
}