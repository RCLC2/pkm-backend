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

    @Test
    void createNewNote_success() {
        // given
        NoteRequestVo vo = new NoteRequestVo("title", "desc", "contents");
        NoteEntity saved = NoteEntity.builder()
                .title("title")
                .description("desc")
                .contents("contents")
                .build();

        when(noteRepository.save(any(NoteEntity.class))).thenReturn(saved);

        // when
        NoteResponseVo result = noteService.createNewNote(vo,"Bearer test-token");

        // then
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
                .title("old")
                .description("old")
                .contents("old")
                .build();

        when(noteRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(existing));
        when(noteRepository.save(any(NoteEntity.class))).thenReturn(existing);

        // when
        NoteResponseVo result = noteService.updateNote(id, new NoteRequestVo("new", "new", "new"));

        // then
        assertThat(result.id()).isEqualTo(id);
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
        assertThatThrownBy(() -> noteService.updateNote("notfound", new NoteRequestVo("t", "d", "c")))
                .isInstanceOf(ServiceException.class)
                .hasMessage(ExceptionStatus.NOTE_NOT_FOUND.getMessage());
    }

    @Test
    void findNoteDetails_success() {
        // given
        String id = "123";
        NoteEntity existing = NoteEntity.builder()
                .id(id)
                .title("title")
                .description("desc")
                .contents("contents")
                .build();

        when(noteRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(existing));

        // when
        NoteResponseVo result = noteService.findNoteDetails(id);

        // then
        assertThat(result.id()).isEqualTo(id);
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

}

