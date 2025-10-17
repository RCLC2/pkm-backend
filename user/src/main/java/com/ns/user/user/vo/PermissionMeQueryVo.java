package com.ns.user.user.vo;

public record PermissionMeQueryVo(
        String noteId,
        String userId
) {
    public static PermissionMeQueryVo of(String noteId, String userId) {
        return new PermissionMeQueryVo(noteId, userId);
    }
}
