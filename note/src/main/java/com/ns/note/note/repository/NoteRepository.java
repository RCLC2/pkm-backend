package com.ns.note.note.repository;

import com.ns.note.note.entity.NoteEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface NoteRepository extends ElasticsearchRepository<NoteEntity, String> {
    Optional<NoteEntity> findByIdAndDeletedAtIsNull(String id);
}
