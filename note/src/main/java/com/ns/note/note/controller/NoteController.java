package com.ns.note.note.controller;

import com.ns.note.note.dto.request.NoteCreateRequestDto;
import com.ns.note.note.dto.request.NoteUpdateRequestDto;
import com.ns.note.note.dto.response.NoteResponseDto;
import com.ns.note.note.service.NoteService;
import com.ns.note.note.vo.NoteRequestVo;
import com.ns.note.note.vo.NoteResponseVo;
import com.ns.note.response.GlobalResponseHandler;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import com.ns.note.response.ResponseStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    // 노트 생성
    @PostMapping("/create")
    public ResponseEntity<GlobalResponseHandler<NoteResponseDto>> createNewNote(
            @RequestBody NoteCreateRequestDto dto,
            @RequestHeader("Authorization") String authorization) {
        NoteRequestVo vo = new NoteRequestVo(
                dto.getWorkspaceId(),
                dto.getTitle(),
                dto.getDescription(),
                dto.getContents()
        );

        NoteResponseVo responseVo = noteService.createNewNote(vo, authorization);

        return GlobalResponseHandler.success(
                ResponseStatus.NOTE_CREATE_SUCCESS,
                NoteResponseDto.from(responseVo) // VO → ResponseDto
        );
    }

    // 노트 수정
    @PutMapping("/update/{id}")
    public  ResponseEntity<GlobalResponseHandler<NoteResponseDto>> updateNote(
            @PathVariable @NotBlank String id,
            @RequestBody NoteUpdateRequestDto dto,
            @RequestHeader("Authorization") String authorization) {

        NoteRequestVo vo = new NoteRequestVo(
                dto.getWorkspaceId(),
                dto.getTitle(),
                dto.getDescription(),
                dto.getContents()
        );

        NoteResponseVo note = noteService.updateNote(id, vo, authorization);

        return GlobalResponseHandler.success(
                ResponseStatus.NOTE_UPDATE_SUCCESS,
                NoteResponseDto.from(note)
        );
    }

    // 노트 상세 조회 (id)
    @GetMapping("/{id}")
    public  ResponseEntity<GlobalResponseHandler<NoteResponseDto>> findNoteDetails(
            @PathVariable @NotBlank String id,
            @RequestHeader("Authorization") String authorization) {
        NoteResponseVo note = noteService.findNoteDetails(id, authorization);

        return GlobalResponseHandler.success(
                ResponseStatus.NOTE_SEARCH_SUCCESS,
                NoteResponseDto.from(note)
        );
    }

    // 노트 삭제 (소프트)
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<GlobalResponseHandler<Void>> deleteNote(
            @PathVariable @NotBlank String id,
            @RequestHeader("Authorization") String authorization) {
        noteService.deleteNote(id, authorization);
        return GlobalResponseHandler.success(ResponseStatus.NOTE_DELETE_SUCCESS);
    }

    // 해당 워크스페이스 내의 모든 문서 ID 리스트 반환
    @GetMapping("/ids")
    public ResponseEntity<GlobalResponseHandler<List<String>>> getAllNoteIdsByWorkspace(
            @RequestParam @NotBlank String workspaceId) {
        List<String> noteIds = noteService.getAllNoteIdsByWorkspace(workspaceId);
        return GlobalResponseHandler.success(ResponseStatus.NOTE_SEARCH_SUCCESS,noteIds);
    }

}
