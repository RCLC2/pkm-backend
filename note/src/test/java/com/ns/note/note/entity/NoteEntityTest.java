package com.ns.note.note.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class NoteEntityTest{

    @Test
    void update_shouldChangeFields() {
        // given
        NoteEntity note = NoteEntity.builder()
                .title("old title")
                .description("old desc")
                .contents("old contents")
                .build();

        // when
        note.update("new title", "new desc", "new contents");

        // then
        assertThat(note.getTitle()).isEqualTo("new title");
        assertThat(note.getDescription()).isEqualTo("new desc");
        assertThat(note.getContents()).isEqualTo("new contents");
    }

    @Test
    void softDelete_shouldSetDeletedAt() {
        // given
        NoteEntity note = NoteEntity.builder()
                .title("title")
                .description("desc")
                .contents("contents")
                .build();

        // when
        note.softDelete();

        // then
        assertThat(note.getDeletedAt()).isNotNull();
        assertThat(note.getDeletedAt()).isBeforeOrEqualTo(Instant.now());
    }
}
