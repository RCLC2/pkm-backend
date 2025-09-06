package com.ns.note.note.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Document(collection = "note")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteEntity {

    @Id
    private String id;

    private String title;
    private String description;
    private String contents;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public void update(String title, String description, String contents) {
        this.title = title;
        this.description = description;
        this.contents = contents;
        this.updatedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

}
