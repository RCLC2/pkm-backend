package com.ns.user.user.vo;

public record YorkieTokenIssueVo(String noteId, String requesterUserId) {
    public static YorkieTokenIssueVo of(String noteId, String requesterUserId) {
        return new YorkieTokenIssueVo(noteId, requesterUserId);
    }
}
