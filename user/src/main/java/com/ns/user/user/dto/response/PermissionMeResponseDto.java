package com.ns.user.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PermissionMeResponseDto {
    private String noteId;
    private String userId;
    private String role;

    public static PermissionMeResponseDto of(String noteId,String userId ,String role){
        return new PermissionMeResponseDto(
                noteId,
                userId,
                role
        );
    }
}
