package com.ns.user.user.service;

import com.ns.user.exception.ServiceException;
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

import static com.ns.user.exception.ExceptionStatus.*;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final StringRedisTemplate redis;

    @Transactional
    public void registerOwner(OwnerRegisterVo ownerRegisterVo) {
        // 중복 요청 시 예외 처리
        try {
            permissionRepository.save(
                    PermissionEntity.builder()
                            .noteId(ownerRegisterVo.noteId())
                            .userId(ownerRegisterVo.userId())
                            .role(PermissionRole.OWNER)
                            .build()
            );
        } catch (DuplicateKeyException e) {
            throw new ServiceException(PERMISSION_ALREADY_EXISTS);
        }

        // Redis 반영
        String ownerKey = "note:owner:" + ownerRegisterVo.noteId();
        redis.opsForValue().set(ownerKey, ownerRegisterVo.userId());
    }

    @Transactional
    public void grantPermission(GrantPermissionVo vo) {
        //  OWNER 검증 (필수)
        ensureOwner(vo.noteId(), vo.requesterId());

        if (vo.role() == PermissionRole.OWNER) {
            throw new ServiceException(PERMISSION_CANNOT_CHANGE_OWNER);
        }

        PermissionRole role = vo.role();
        // 중복된 요청인지 검증
        if (permissionRepository.existsByNoteIdAndUserIdAndRoleAndDeletedAtIsNull(
                vo.noteId(), vo.targetUserId(), role)) {
            throw new ServiceException(PERMISSION_ALREADY_EXISTS);
        }

        // mongo 반영
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
        }
    }

    @Transactional
    public void revokePermission(RevokePermissionVo vo) {
        //  OWNER 검증 (필수)
        ensureOwner(vo.noteId(), vo.requesterId());

        if (vo.role() == PermissionRole.OWNER) {
            throw new ServiceException(PERMISSION_CANNOT_CHANGE_OWNER);
        }

        // 사용자, 노트, 권한 정보가 담긴 entity 조회
        PermissionEntity permission = permissionRepository
                .findByNoteIdAndUserIdAndRoleAndDeletedAtIsNull(
                        vo.noteId(), vo.targetUserId(), vo.role())
                .orElseThrow(() -> new ServiceException(PERMISSION_NOT_FOUND));

        // 소프트 딜리트
        permission.softDelete();
        permissionRepository.save(permission);

        // Redis 제거
        switch (vo.role()) {
            case WRITER -> redis.opsForSet().remove("note:writers:" + vo.noteId(), vo.targetUserId());
            case READER -> redis.opsForSet().remove("note:readers:" + vo.noteId(), vo.targetUserId());
        }
    }

    // redis 우선 -> mongo fallback
    private void ensureOwner(String noteId, String requesterId) {
        String owner = redis.opsForValue().get("note:owner:" + noteId);
        if (owner != null) {
            // 노트 OWNER 불일치 예외
            if (!owner.equals(requesterId))
                throw new ServiceException(PERMISSION_OWNER_ONLY);
            return;
        }
        // redis 값이 없을 경우 mongo db에서 존재여부 확인
        boolean isOwner = permissionRepository
                .existsByNoteIdAndUserIdAndRoleAndDeletedAtIsNull(noteId, requesterId, PermissionRole.OWNER);
        // 노트 OWNER 불일치 예외
        if (!isOwner) throw new ServiceException(PERMISSION_OWNER_ONLY);

        // 노트 OWNER 정보가 mongoDB에 있을경우 다시 redis에 정보 캐싱
        String ownerKey = "note:owner:" + noteId;
        redis.opsForValue().set(ownerKey, requesterId);
    }
}
