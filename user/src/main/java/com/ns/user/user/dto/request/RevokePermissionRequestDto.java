package com.ns.user.user.dto.request;

import com.ns.user.exception.DtoException;
import com.ns.user.user.entity.PermissionRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.ns.user.exception.ExceptionStatus.GENERAL_REQUEST_INVALID_ROLE;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RevokePermissionRequestDto {
    private String targetUserId;
    private String role;

    public PermissionRole toPermissionRole() {
        try {
            // String을 대문자로 변환하여 ENUM으로 매핑 시도
            return PermissionRole.valueOf(this.role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DtoException(GENERAL_REQUEST_INVALID_ROLE);
        }
    }
}
