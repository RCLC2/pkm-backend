package com.ns.note.note.service;

import com.ns.note.exception.ExceptionStatus;
import com.ns.note.exception.ServiceException;
import com.ns.note.note.dto.request.OwnerRegisterRequestDto;
import com.ns.note.note.entity.NoteEntity;
import com.ns.note.note.repository.NoteRepository;
import com.ns.note.note.vo.NoteRequestVo;
import com.ns.note.note.vo.NoteResponseVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static com.ns.note.exception.ExceptionStatus.OWNER_PERMISSION_REGISTER_FAILED;
import static com.ns.note.exception.ExceptionStatus.USER_SERVICE_ACCESS_FAILED;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final RestTemplate restTemplate;

    @Value("${services.user.base-url}")
    private String userBaseUrl;

    public NoteResponseVo createNewNote(NoteRequestVo vo, String authorization) {
        NoteEntity entity = NoteEntity.builder()
                .workspaceId(vo.workspaceId())
                .title(vo.title())
                .description(vo.description())
                .contents(vo.contents())
                .build();

        NoteEntity newNote = noteRepository.save(entity);

        // USER 서비스로 OWNER 정보 저장 요청 ( MVP 기준, 나중에 관심사 분리 )
        try {
            registerOwner(newNote.getId(), authorization);
        } catch (HttpStatusCodeException e) { // 4xx / 5xx 모두 처리
            noteRepository.delete(newNote);
            throw new ServiceException(OWNER_PERMISSION_REGISTER_FAILED);
        } catch (ResourceAccessException e) { // 연결 문제
            noteRepository.delete(newNote);
            throw new ServiceException(USER_SERVICE_ACCESS_FAILED);
        }

        return NoteEntitytoNoteResponseVo(newNote);
    }

    public NoteResponseVo updateNote(String id, NoteRequestVo vo) {
        NoteEntity note = getActiveNoteById(id);

        note.update(vo.workspaceId(), vo.title(), vo.description(), vo.contents());

        NoteEntity updatedNote = noteRepository.save(note);

        return NoteEntitytoNoteResponseVo(updatedNote);
    }

    public NoteResponseVo findNoteDetails(String id) {
        NoteEntity note = getActiveNoteById(id);

        return NoteEntitytoNoteResponseVo(note);
    }

    public void deleteNote(String id) {
        NoteEntity note = getActiveNoteById(id);

        note.softDelete();

        noteRepository.save(note);
    }

    // 삭제되지 않은 노트 조회
    private NoteEntity getActiveNoteById(String id) {
        return noteRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ServiceException(ExceptionStatus.NOTE_NOT_FOUND));
    }

    private NoteResponseVo NoteEntitytoNoteResponseVo(NoteEntity note) {
        return new NoteResponseVo(
                note.getId(),
                note.getWorkspaceId(),
                note.getTitle(),
                note.getDescription(),
                note.getContents(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }

    private void registerOwner(String noteId, String bearer) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", bearer);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<OwnerRegisterRequestDto> entity =
                new HttpEntity<>(new OwnerRegisterRequestDto(noteId), headers);

        restTemplate.exchange(
                userBaseUrl + "/permission/owner/register",
                HttpMethod.POST,
                entity,
                Void.class
        );
    }

    public List<String> getAllNoteIdsByWorkspace(String workspaceId) {
        return noteRepository.findAllByWorkspaceIdAndDeletedAtIsNull(workspaceId)
                .stream()
                .map(NoteEntity::getId)
                .collect(Collectors.toList());

    }

    public List<NoteResponseVo> searchNotesByKeyword(String workspaceId, String keyword, Integer limit) {
        List<NoteEntity> entities = noteRepository.searchByKeywordAndWorkspaceId(workspaceId, keyword, limit);

        return entities.stream()
                .map(this::NoteEntitytoNoteResponseVo)
                .collect(Collectors.toList());
    }

    public List<NoteResponseVo> findRecentUpdatedNotes(String workspaceId, Integer limit) {
        List<NoteEntity> entities = noteRepository.findRecentByWorkspaceId(workspaceId, limit);

        return entities.stream()
                .map(this::NoteEntitytoNoteResponseVo)
                .collect(Collectors.toList());
    }
}
