package com.ns.user.user.vo;

import com.ns.user.user.entity.PermissionRole;

public record RevokePermissionVo(String noteId, String targetUserId, PermissionRole role, String requesterId) {
    public static RevokePermissionVo of(String noteId, String targetUserId, PermissionRole role, String requesterId) {
        return new RevokePermissionVo(noteId, targetUserId, role, requesterId);
    }
}