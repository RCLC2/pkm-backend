package com.ns.note.note.repository;

import com.ns.note.note.entity.NoteEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
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

    List<NoteEntity> findAllByWorkspaceIdAndDeletedAtIsNull(String workspaceId);

    @Query("""
    {
      "bool": {
        "filter": [
          { "term": { "workspaceId.keyword": "?0" } }
        ],
        "must_not": [
          { "exists": { "field": "deletedAt" } }
        ],
        "should": [
          {
            "multi_match": {
              "query": "?1",
              "fields": ["title^3", "contents"],
              "type": "best_fields"
            }
          }
        ],
        "minimum_should_match": 0
      }
    }
    """)
    List<NoteEntity> searchByKeywordAndWorkspaceId(String workspaceId, String keyword, Pageable pageable);

    List<NoteEntity> findAllByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String workspaceId, Pageable pageable);

}