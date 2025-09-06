package com.ns.note.note.repository;

import com.ns.note.note.entity.NoteEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NoteRepository extends MongoRepository<NoteEntity, String> {
    Optional<NoteEntity> findByIdAndDeletedAtIsNull(String id);
}
