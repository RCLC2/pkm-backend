package com.ns.note.note.repository;

import com.ns.note.note.entity.NoteEntity;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface NoteRepository extends ElasticsearchRepository<NoteEntity, String> {
    @Query("""
        {
          "bool": {
            "must": [
              { "term": { "_id": "?0" } }
            ],
            "must_not": [
              { "exists": { "field": "deletedAt" } }
            ]
          }
        }
    """)
    Optional<NoteEntity> findByIdAndDeletedAtIsNull(String id);
}
