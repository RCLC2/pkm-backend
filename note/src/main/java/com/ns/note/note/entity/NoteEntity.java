package com.ns.note.note.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;

@Setting(settingPath = "elasticsearch/note-settings.json")
@Document(indexName = "note")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteEntity {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String workspaceId;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String title;
    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String description;
    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String contents;

    @Field(type = FieldType.Keyword)
    private ParaCategory paraCategory;

    @CreatedDate
    @Field(type = FieldType.Date_Nanos, format = DateFormat.date_time)
    private Instant createdAt;

    @LastModifiedDate
    @Field(type = FieldType.Date_Nanos, format = DateFormat.date_time)
    private Instant updatedAt;

    @Field(type = FieldType.Date_Nanos, format = DateFormat.date_time)
    private Instant deletedAt;

    public void update(String workspaceId, String title, String description, String contents, ParaCategory paraCategory) {
        this.workspaceId = workspaceId;
        this.title = title;
        this.description = description;
        this.contents = contents;
        this.paraCategory = paraCategory;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

}
