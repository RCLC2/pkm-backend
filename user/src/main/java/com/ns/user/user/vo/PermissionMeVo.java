package com.ns.user.user.vo;

import com.ns.user.user.entity.PermissionRole;

public record PermissionMeVo(
        String noteId,
        String userId,
        PermissionRole role
) {
    public static PermissionMeVo of(String noteId, String userId, PermissionRole role) {
        return new PermissionMeVo(noteId, userId, role);
    }
}