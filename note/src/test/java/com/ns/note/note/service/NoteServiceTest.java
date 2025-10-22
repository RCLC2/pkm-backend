package com.ns.note.note.service;


import com.ns.note.exception.ExceptionStatus;
import com.ns.note.exception.ServiceException;
import com.ns.note.note.entity.NoteEntity;
import com.ns.note.note.repository.NoteRepository;
import com.ns.note.note.vo.NoteRequestVo;
import com.ns.note.note.vo.NoteResponseVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

// Mockito 를 활용한 기본적인 단위 테스트 코드
@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @InjectMocks
    private NoteService noteService;

    @Mock
    private RestTemplate restTemplate;

    @Test
    void createNewNote_success() {
        // given
        NoteRequestVo vo = new NoteRequestVo("1", "title", "desc", "contents");
        NoteEntity saved = NoteEntity.builder()
        .workspaceId("1")
                .title("title")
                .description("desc")
                .contents("contents")
                .build();

        when(noteRepository.save(any(NoteEntity.class))).thenReturn(saved);

        // 🔧 RestTemplate Mock 응답 (registerOwner 호출 시)
        when(restTemplate.exchange(anyString(), any(), any(), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        // when
        NoteResponseVo result = noteService.createNewNote(vo,"Bearer test-token");

        // then
        assertThat(result.workspaceId()).isEqualTo("1");
        assertThat(result.title()).isEqualTo("title");
        assertThat(result.contents()).isEqualTo("contents");
        assertThat(result.description()).isEqualTo("desc");

        verify(noteRepository, times(1)).save(any(NoteEntity.class));
    }

    @Test
    void updateNote_success() {
        // given
        String id = "123";
        NoteEntity existing = NoteEntity.builder()
                .id(id)
                .workspaceId("1")
                .title("old")
                .description("old")
                .contents("old")
                .build();

        when(noteRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(existing));
        when(noteRepository.save(any(NoteEntity.class))).thenReturn(existing);

        // when
        NoteResponseVo result = noteService.updateNote(id, new NoteRequestVo("1", "new", "new", "new"));

        // then
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.workspaceId()).isEqualTo("1");
        assertThat(result.title()).isEqualTo("new");
        assertThat(result.description()).isEqualTo("new");
        assertThat(result.contents()).isEqualTo("new");
        verify(noteRepository, times(1)).save(existing);
    }

    @Test
    void updateNote_notFound_shouldThrowException() {
        // given
        when(noteRepository.findByIdAndDeletedAtIsNull("notfound")).thenReturn(Optional.empty());

        // expect
        assertThatThrownBy(() -> noteService.updateNote("notfound", new NoteRequestVo("1", "t", "d", "c")))
                .isInstanceOf(ServiceException.class)
                .hasMessage(ExceptionStatus.NOTE_NOT_FOUND.getMessage());
    }

    @Test
    void findNoteDetails_success() {
        // given
        String id = "123";
        NoteEntity existing = NoteEntity.builder()
                .id(id)
                .workspaceId("1")
                .title("title")
                .description("desc")
                .contents("contents")
                .build();

        when(noteRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(existing));

        // when
        NoteResponseVo result = noteService.findNoteDetails(id);

        // then
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.workspaceId()).isEqualTo("1");
        assertThat(result.title()).isEqualTo("title");
        assertThat(result.description()).isEqualTo("desc");
        assertThat(result.contents()).isEqualTo("contents");

    }

    @Test
    void findNoteDetails_notFound_shouldThrowException() {
        when(noteRepository.findByIdAndDeletedAtIsNull("notfound")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.findNoteDetails("notfound"))
                .isInstanceOf(ServiceException.class)
                .hasMessage(ExceptionStatus.NOTE_NOT_FOUND.getMessage());
    }


    @Test
    void deleteNote_success() {
        // given
        String id = "123";
        NoteEntity existing = NoteEntity.builder()
                .id(id)
                .workspaceId("1")
                .title("title")
                .description("desc")
                .contents("contents")
                .build();

        when(noteRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(existing));

        // when
        noteService.deleteNote(id);

        // then
        assertThat(existing.getDeletedAt()).isNotNull();
        verify(noteRepository, times(1)).save(existing);
    }

    @Test
    void deleteNote_notFound_shouldThrowException() {
        when(noteRepository.findByIdAndDeletedAtIsNull("notfound")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.deleteNote("notfound"))
                .isInstanceOf(ServiceException.class)
                .hasMessage(ExceptionStatus.NOTE_NOT_FOUND.getMessage());
    }

    @Test
    void searchNotesByKeyword_withPageable_success() {
        // given
        String workspaceId = "ws1";
        String keyword = "keyword";
        Pageable pageable = PageRequest.of(0, 5, Sort.by("updatedAt").descending());

        NoteEntity note1 = NoteEntity.builder().id("n1").title("keyword-title").workspaceId(workspaceId).build();
        List<NoteEntity> mockEntities = List.of(note1);

        when(noteRepository.searchByKeywordAndWorkspaceId(eq(workspaceId), eq(keyword), eq(pageable))).thenReturn(mockEntities);

        // when
        List<NoteResponseVo> result = noteService.searchNotesByKeyword(workspaceId, keyword, pageable);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("n1");
        verify(noteRepository, times(1)).searchByKeywordAndWorkspaceId(eq(workspaceId), eq(keyword), eq(pageable));
    }


    @Test
    void findRecentUpdatedNotes_withPageable_success() {
        // given
        String workspaceId = "ws1";
        Pageable pageable = PageRequest.of(0, 3, Sort.by("updatedAt").descending());

        NoteEntity noteA = NoteEntity.builder().id("nA").title("titleA").workspaceId(workspaceId).build();
        NoteEntity noteB = NoteEntity.builder().id("nB").title("titleB").workspaceId(workspaceId).build();
        List<NoteEntity> mockEntities = List.of(noteA, noteB);

        when(noteRepository.findAllByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(eq(workspaceId), eq(pageable))).thenReturn(mockEntities);

        // when
        List<NoteResponseVo> result = noteService.findRecentUpdatedNotes(workspaceId, pageable);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("titleA");
        verify(noteRepository, times(1)).findAllByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(eq(workspaceId), eq(pageable));
    }
}

