package com.ns.note.note.service;

import com.ns.note.exception.ExceptionStatus;
import com.ns.note.exception.ServiceException;
import com.ns.note.note.entity.NoteEntity;
import com.ns.note.note.repository.NoteRepository;
import com.ns.note.note.vo.NoteRequestVo;
import com.ns.note.note.vo.NoteResponseVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;

    public NoteResponseVo createNewNote(NoteRequestVo vo) {
        NoteEntity entity = NoteEntity.builder()
                .title(vo.title())
                .description(vo.description())
                .contents(vo.contents())
                .build();

        NoteEntity newNote = noteRepository.save(entity);

        return NoteEntitytoNoteResponseVo(newNote);
    }

    public NoteResponseVo updateNote(String id, NoteRequestVo vo) {
        NoteEntity note = getActiveNoteById(id);


        note.update(vo.title(), vo.description(), vo.contents());

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
                note.getTitle(),
                note.getDescription(),
                note.getContents(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
