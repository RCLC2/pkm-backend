package com.ns.user.user.vo;

import com.ns.user.user.entity.PermissionRole;

public record GrantPermissionVo(String noteId, String targetUserId, PermissionRole role, String requesterId) {
    public static GrantPermissionVo of(String noteId, String targetUserId, PermissionRole role, String requesterId) {
        return new GrantPermissionVo(noteId, targetUserId, role, requesterId);
    }
}