package com.ns.user.user.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ns.user.user.dto.DocumentAttributeDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class YorkieAuthWebhookRequestDto {
    @JsonProperty("token")
    private String token;   // yorkie_jwt
    @JsonProperty("method") 
    private String method;  // "PushPull"
    @JsonProperty("attributes")
    private List<DocumentAttributeDto> attributes;
}
