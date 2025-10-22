package com.ns.note.note.repository;

import com.ns.note.note.entity.NoteEntity;
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
          "size": "?2",
          "query": {
            "bool": {
              "must": [
                { "term": { "workspaceId": "?0" } }
              ],
              "must_not": [
                { "exists": { "field": "deletedAt" } }
              ],
              "should": [
                { "match": { "title": "?1" } },
                { "match": { "contents": "?1" } }
              ],
              "minimum_should_match": 1
            }
          }
        }
    """)
    List<NoteEntity> searchByKeywordAndWorkspaceId(String workspaceId, String keyword, Integer limit);

    @Query("""
        {
          "size": "?1",
          "query": {
            "bool": {
              "must": [
                { "term": { "workspaceId": "?0" } }
              ],
              "must_not": [
                { "exists": { "field": "deletedAt" } }
              ]
            }
          },
          "sort": [
            { "updatedAt": { "order": "desc" } }
          ]
        }
    """)
    List<NoteEntity> findRecentByWorkspaceId(String workspaceId, Integer limit);
}