package com.ns.user.user.vo;

public record OwnerRegisterVo(String noteId, String userId) {
    public static OwnerRegisterVo of(String noteId, String userId) {
        return new OwnerRegisterVo(noteId, userId);
    }
}