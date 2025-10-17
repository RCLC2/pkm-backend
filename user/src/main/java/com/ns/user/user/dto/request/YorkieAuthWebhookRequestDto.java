package com.ns.user.user.dto.request;

import com.ns.user.user.dto.DocumentAttributeDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class YorkieAuthWebhookRequestDto {
    private String token;   // yorkie_jwt
    private String method;  // "PushPull"
    private List<DocumentAttributeDto> documentAttributes;
}
