package com.ns.user.user.repository;

import com.ns.user.user.entity.PermissionEntity;
import com.ns.user.user.entity.PermissionRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface PermissionRepository extends MongoRepository<PermissionEntity, String> {

    boolean existsByNoteIdAndUserIdAndRoleAndDeletedAtIsNull(
            String noteId, String userId, PermissionRole role);

    Optional<PermissionEntity> findByNoteIdAndUserIdAndRoleAndDeletedAtIsNull(
            String noteId, String userId, PermissionRole role);

    @Query("{ 'noteId': ?0, 'userId': ?1, 'deletedAt': null }")
    Optional<PermissionEntity> findPermissionRoleByNoteIdAndUserIdAndDeletedAtIsNull(String noteId, String userId);
}
