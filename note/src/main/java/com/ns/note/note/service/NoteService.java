package com.ns.note.note.service;

import com.ns.note.exception.ExceptionStatus;
import com.ns.note.exception.ServiceException;
import com.ns.note.note.dto.request.OwnerRegisterRequestDto;
import com.ns.note.note.dto.response.ResponseHandlerDto;
import com.ns.note.note.entity.NoteEntity;
import com.ns.note.note.repository.NoteRepository;
import com.ns.note.note.vo.NoteParaMappingVo;
import com.ns.note.note.vo.NoteRequestVo;
import com.ns.note.note.vo.NoteResponseVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ns.note.exception.ExceptionStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final RestTemplate restTemplate;

    @Value("${services.user.base-url}")
    private String userBaseUrl;

    @Value("${services.graph.base-url}")
    private String graphBaseUrl;

    public NoteResponseVo createNewNote(NoteRequestVo vo, String authorization) {
        NoteEntity entity = NoteEntity.builder()
                .workspaceId(vo.workspaceId())
                .title(vo.title())
                .description(vo.description())
                .paraCategory(vo.paraCategory())
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

    public NoteResponseVo updateNote(String id, NoteRequestVo vo,  String authorization) {
        String role = getMyRole(id, authorization);
        if(!(role.equals("OWNER")||role.equals("WRITER"))){
            throw new ServiceException(ExceptionStatus.NOTE_SERVICE_NOT_AUTHENTICATION_ROLE);
        }

        NoteEntity note = getActiveNoteById(id);

        note.update(vo.workspaceId(), vo.title(), vo.description(), vo.contents(),vo.paraCategory());

        NoteEntity updatedNote = noteRepository.save(note);

        return NoteEntitytoNoteResponseVo(updatedNote);
    }

    public NoteResponseVo findNoteDetails(String id, String authorization) {
        String role = getMyRole(id, authorization);
        if(!(role.equals("OWNER")||role.equals("WRITER")||role.equals("READER"))){
            throw new ServiceException(ExceptionStatus.NOTE_SERVICE_NOT_AUTHENTICATION_ROLE);
        }

        NoteEntity note = getActiveNoteById(id);

        return NoteEntitytoNoteResponseVo(note);
    }

    public void deleteNote(String id, String authorization) {
        String role = getMyRole(id, authorization);
        if(!(role.equals("OWNER"))) {
            throw new ServiceException(ExceptionStatus.NOTE_SERVICE_NOT_AUTHENTICATION_ROLE);
        }

        NoteEntity note = getActiveNoteById(id);
        boolean graphDeleted = deleteNoteGraphLinks(note.getId(), note.getWorkspaceId());
        if (!graphDeleted) {
            throw new ServiceException(ExceptionStatus.GENERAL_INTERNAL_SERVER_ERROR);
        }
        note.softDelete();

        noteRepository.save(note);
    }

    public List<String> getAllNoteIdsByWorkspace(String workspaceId) {
        return noteRepository.findAllByWorkspaceIdAndDeletedAtIsNull(workspaceId)
                .stream()
                .map(NoteEntity::getId)
                .collect(Collectors.toList());

    }

    public List<NoteResponseVo> searchNotesByKeyword(String workspaceId, String keyword, Pageable pageable) {
        log.info("Parameters: workspaceId={}, keyword='{}', Pageable={}", workspaceId, keyword, pageable.toString());
        List<NoteEntity> entities = noteRepository.searchByKeywordAndWorkspaceId(workspaceId, keyword, pageable);

        return entities.stream()
                .map(this::NoteEntitytoNoteResponseVo)
                .collect(Collectors.toList());
    }

    public List<NoteResponseVo> findRecentUpdatedNotes(String workspaceId, Pageable pageable) {
        log.info("Parameters: workspaceId={}, Pageable={}", workspaceId, pageable.toString());
        List<NoteEntity> entities = noteRepository.findAllByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(workspaceId, pageable);

        return entities.stream()
                .map(this::NoteEntitytoNoteResponseVo)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateParaMappings(NoteParaMappingVo vo, String userId) {
        String workspaceId = vo.workspaceId();

        List<String> allNoteIds = noteRepository.findAllIdsByWorkspaceIdAndDeletedAtIsNull(workspaceId)
                .stream()
                .map(NoteEntity::getId)
                .toList();

        List<String> mappedIds = vo.mappings().stream()
                .map(NoteParaMappingVo.MappingItem::noteId)
                .toList();

        // 순서 무관, 전체 포함 여부 검증
        if (!new HashSet<>(allNoteIds).containsAll(mappedIds) || allNoteIds.size() != mappedIds.size()) {
            throw new ServiceException(ExceptionStatus.NOTE_PARA_MAPPING_INCOMPLETE);
        }

        // 각 노트 업데이트
        for (NoteParaMappingVo.MappingItem item : vo.mappings()) {
            NoteEntity note = getActiveNoteById(item.noteId());

            note.update(
                    note.getWorkspaceId(),
                    note.getTitle(),
                    note.getDescription(),
                    note.getContents(),
                    item.paraCategory()
            );

            noteRepository.save(note);
        }

        String para = "para";
        notifyGraphParaSync(vo.workspaceId(), userId, para);
    }

    private void notifyGraphParaSync(String workspaceId, String userId, String newStyle) {
        String url = graphBaseUrl + "/workspaces/" + workspaceId + "/style";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-ID", userId);

        Map<String, String> body = Map.of("newStyle", newStyle);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            log.info("Workspace style sync request sent: workspaceId={}, newStyle={}", workspaceId, newStyle);

        } catch (HttpStatusCodeException e) {
            log.warn("Workspace style sync failed for workspace {}: {}", workspaceId, e.getResponseBodyAsString());

        } catch (ResourceAccessException e) {
            log.error("Workspace service not reachable for workspaceId={}", workspaceId);
        }
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
                note.getParaCategory(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }

    // USER 서비스에 OWNER 권한 등록 요청
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

    // USER 서비스에서 나의 역할 조회
    private String getMyRole(String noteId, String bearer) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", bearer);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try{
            ResponseEntity<ResponseHandlerDto> response = restTemplate.exchange(
                    userBaseUrl + "/permission/" + noteId + "/me",
                    HttpMethod.GET,
                    entity,
                    ResponseHandlerDto.class
            );

            ResponseHandlerDto body = response.getBody();

            if(body==null || body.getData()==null || body.getData().getRole()==null){
                throw new ServiceException(USER_SERVICE_INVALID_ROLE);
            }

            return body.getData().getRole();

        }catch (HttpStatusCodeException e) { // 4xx / 5xx 모두 처리
            throw new ServiceException(NOTE_SERVICE_PERMISSION_DENIED);
        } catch (ResourceAccessException e) { // 연결 문제
            throw new ServiceException(USER_SERVICE_ACCESS_FAILED);
        }

    }

    public void deleteAllNotesByUserByWorkspaceId(String workspaceId) {
        List<NoteEntity> workspaceNotes = noteRepository.findAllIdsByWorkspaceIdAndDeletedAtIsNull(workspaceId);

        for (NoteEntity note : workspaceNotes) {
            try {
                deleteNoteGraphLinks(note.getId(), workspaceId);
            } catch (Exception e) {
                log.warn("Failed to delete graph links for note: {}", note.getId(), e);
            }
            note.softDelete();
        }

        noteRepository.saveAll(workspaceNotes);
    }
    
   private boolean deleteNoteGraphLinks(String noteId, String workspaceId) {
       String url = graphBaseUrl + "/connections/note-deleted";

       Map<String, String> requestBody = new HashMap<>();
       requestBody.put("noteId", noteId);
       requestBody.put("workspaceId", workspaceId);

       try {
           HttpHeaders headers = new HttpHeaders();
           headers.setContentType(MediaType.APPLICATION_JSON);
           HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
           ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

           if (response.getStatusCode().is2xxSuccessful()) {
               Object status = response.getBody() != null ? response.getBody().get("status") : null;
               return "success".equals(status);
           }

           return false;
       } catch (HttpStatusCodeException e) {
           log.warn("failed to deleteNoteGraphLinks {}: {}", noteId, e.getStatusCode());
           return false;
       } catch (ResourceAccessException e) {
           log.error("failed to deleteNoteGraphLinks {}", noteId);
           return false;
       }
   }
}
