package com.ns.note.note.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Document(indexName = "note")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteEntity {
    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String title;
    @Field(type = FieldType.Text)
    private String description;
    @Field(type = FieldType.Text)
    private String contents;

    @CreatedDate
    @Field(type = FieldType.Date_Nanos, format = DateFormat.date_time)
    private Instant createdAt;

    @LastModifiedDate
    @Field(type = FieldType.Date_Nanos, format = DateFormat.date_time)
    private Instant updatedAt;

    @Field(type = FieldType.Date_Nanos, format = DateFormat.date_time)
    private Instant deletedAt;

    public void update(String title, String description, String contents) {
        this.title = title;
        this.description = description;
        this.contents = contents;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

}
