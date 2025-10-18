package com.ns.user.user.dto.response;

import com.ns.user.user.dto.DocumentAttributeDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class YorkieTokenResponseDto {
    private String token;
    private int expiresIn; // seconds
    private DocumentAttributeDto documentAttributes;
}