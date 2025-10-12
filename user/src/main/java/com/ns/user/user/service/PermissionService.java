package com.ns.user.user.service;

import org.springframework.dao.DuplicateKeyException;
import com.ns.user.user.entity.PermissionEntity;
import com.ns.user.user.entity.PermissionRole;
import com.ns.user.user.repository.PermissionRepository;
import com.ns.user.user.vo.GrantPermissionVo;
import com.ns.user.user.vo.OwnerRegisterVo;
import com.ns.user.user.vo.RevokePermissionVo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final StringRedisTemplate redis;

    @Transactional
    public void registerOwner(OwnerRegisterVo ownerRegisterVo) {

        try {
            permissionRepository.save(
                    PermissionEntity.builder()
                            .noteId(ownerRegisterVo.noteId())
                            .userId(ownerRegisterVo.userId())
                            .role(PermissionRole.OWNER)
                            .build()
            );
        } catch (DuplicateKeyException e) {
            throw new ServiceException(ExceptionStatus.PERMISSION_ALREADY_EXISTS);
        }

        // Redis 반영
        String ownerKey = "note:owner:" + ownerRegisterVo.noteId();
        redis.opsForValue().set(ownerKey, ownerRegisterVo.userId());
    }

    @Transactional
    public void grantPermission(GrantPermissionVo vo) {
        // ✅ OWNER 검증 (필수)
        ensureOwner(vo.noteId(), vo.requesterId());

        PermissionRole role = vo.role();
        if (permissionRepository.existsByNoteIdAndUserIdAndRoleAndDeletedAtIsNull(
                vo.noteId(), vo.targetUserId(), role)) {
            throw new ServiceException(ExceptionStatus.PERMISSION_ALREADY_EXISTS);
        }

        permissionRepository.save(
                PermissionEntity.builder()
                        .noteId(vo.noteId())
                        .userId(vo.targetUserId())
                        .role(role)
                        .build()
        );

        // Redis 반영
        switch (role) {
            case WRITER -> redis.opsForSet().add("note:writers:" + vo.noteId(), vo.targetUserId());
            case READER -> redis.opsForSet().add("note:readers:" + vo.noteId(), vo.targetUserId());
            case OWNER  -> throw new ServiceException(ExceptionStatus.GENERAL_BAD_REQUEST);
        }
    }

    @Transactional
    public void revokePermission(RevokePermissionVo vo) {
        // ✅ OWNER 검증 (필수)
        ensureOwner(vo.noteId(), vo.requesterId());

        if (vo.role() == PermissionRole.OWNER) {
            throw new ServiceException(ExceptionStatus.PERMISSION_CANNOT_REVOKE_OWNER);
        }

        // ✅ role까지 포함해서 정확히 한 권한만 회수
        PermissionEntity permission = permissionRepository
                .findByNoteIdAndUserIdAndRoleAndDeletedAtIsNull(
                        vo.noteId(), vo.targetUserId(), vo.role())
                .orElseThrow(() -> new ServiceException(ExceptionStatus.PERMISSION_NOT_FOUND));

        permission.softDelete();
        permissionRepository.save(permission);

        // Redis 제거
        switch (vo.role()) {
            case WRITER -> redis.opsForSet().remove("note:writers:" + vo.noteId(), vo.targetUserId());
            case READER -> redis.opsForSet().remove("note:readers:" + vo.noteId(), vo.targetUserId());
            case OWNER  -> { /* 위에서 이미 차단 */ }
        }
    }

    // redis 우선 -> mongo fallback
    private void ensureOwner(String noteId, String requesterId) {
        String owner = redis.opsForValue().get("note:owner:" + noteId);
        if (owner != null) {
            if (!owner.equals(requesterId))
                throw new ServiceException(ExceptionStatus.PERMISSION_OWNER_ONLY);
            return;
        }
        boolean isOwner = permissionRepository
                .existsByNoteIdAndUserIdAndRoleAndDeletedAtIsNull(noteId, requesterId, PermissionRole.OWNER);
        if (!isOwner) throw new ServiceException(ExceptionStatus.PERMISSION_OWNER_ONLY);

        String ownerKey = "note:owner:" + noteId;
        redis.opsForValue().set(ownerKey, requesterId);
    }
}
