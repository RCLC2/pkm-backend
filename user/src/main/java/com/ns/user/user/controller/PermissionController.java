package com.ns.user.user.controller;


import com.ns.user.jwt.CurrentUser;
import com.ns.user.response.GlobalResponseHandler;
import com.ns.user.user.dto.request.GrantPermissionRequestDto;
import com.ns.user.user.dto.request.OwnerRegisterRequestDto;
import com.ns.user.user.dto.request.RevokePermissionRequestDto;
import com.ns.user.user.entity.PermissionRole;
import com.ns.user.user.service.PermissionService;
import com.ns.user.user.vo.GrantPermissionVo;
import com.ns.user.user.vo.OwnerRegisterVo;
import com.ns.user.user.vo.RevokePermissionVo;
import com.ns.user.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/permission")
@RequiredArgsConstructor
public class PermissionController {
    private final PermissionService permissionService;

    @PostMapping("/owner/register")
    public ResponseEntity<GlobalResponseHandler<Void>> registerOwner(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestBody OwnerRegisterRequestDto request
    ) {
        permissionService.registerOwner(
                OwnerRegisterVo.of(request.getNoteId(), currentUser.id())
        );
        return GlobalResponseHandler.success(ResponseStatus.PERMISSION_OWNER_REGISTER_SUCCESS);
    }

    @PostMapping("/{noteId}/grant")
    public ResponseEntity<GlobalResponseHandler<Void>> grantPermission(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String noteId,
            @RequestBody GrantPermissionRequestDto request
    ) {
        permissionService.grantPermission(
                GrantPermissionVo.of(noteId, request.getTargetUserId(), request.toPermissionRole(), currentUser.id())
        );
        return GlobalResponseHandler.success(ResponseStatus.PERMISSION_GRANT_SUCCESS);
    }

    @PostMapping("/{noteId}/revoke")
    public ResponseEntity<GlobalResponseHandler<Void>> revokePermission(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String noteId,
            @RequestBody RevokePermissionRequestDto request
    ) {
        permissionService.revokePermission(
                RevokePermissionVo.of(noteId, request.getTargetUserId(), request.toPermissionRole(), currentUser.id())
        );
        return GlobalResponseHandler.success(ResponseStatus.PERMISSION_REVOKE_SUCCESS);
    }

}
