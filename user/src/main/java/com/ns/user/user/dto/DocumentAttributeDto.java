package com.ns.user.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DocumentAttributeDto {
    @JsonProperty("key") 
    private String key; // "note-123"
    @JsonProperty("verb") 
    private String verb;  // "r" | "rw"
}
