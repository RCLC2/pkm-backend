package com.ns.user.user.vo;

import lombok.Builder;

@Builder
public record YorkieAuthResultVo(
        boolean allowed,
        String reason
) {
    public static YorkieAuthResultVo of(boolean allowed, String reason) {
        return new YorkieAuthResultVo(allowed, reason);
    }
}
