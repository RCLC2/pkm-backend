package com.ns.user.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class YorkieAuthWebhookResponseDto {
    @JsonProperty("allowed") 
    private boolean allowed;
    @JsonProperty("reason")
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
