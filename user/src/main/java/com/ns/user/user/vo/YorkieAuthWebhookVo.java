package com.ns.user.user.vo;


public record YorkieAuthWebhookVo(String token,
                                  String method,
                                  String noteId,
                                  String verb) {
    public static YorkieAuthWebhookVo of(
            String token,
            String method,
            String noteId,
            String verb) {
        return new YorkieAuthWebhookVo(token, method, noteId, verb);
    }
}
