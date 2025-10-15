package com.ns.user.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DocumentAttributeDto {
    private String key; // "note:123"
    private String verb;  // "r" | "rw"
}
