package com.ns.user.user.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document("permission_records")
@CompoundIndex(name = "ux_note_user_role", def = "{'noteId':1,'userId':1,'role':1}", unique = true)
public class PermissionEntity {

    @Id
    private String id;

    private String noteId;
    private String userId;

    private PermissionRole role;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;  // soft delete 용

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

}