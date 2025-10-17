package com.ns.user.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class YorkieAuthWebhookResponseDto {
    private boolean allowed;
    private String reason; // 거부 사유(허용이면 null)

    public static YorkieAuthWebhookResponseDto allow() {
        return YorkieAuthWebhookResponseDto.builder()
                .allowed(true)
                .reason(null)
                .build();
    }
    public static YorkieAuthWebhookResponseDto deny(String reason) {
        return YorkieAuthWebhookResponseDto.builder()
                .allowed(false)
                .reason(reason)
                .build();
    }
}
