package com.ns.user.user.controller;


import com.ns.user.jwt.CurrentUser;
import com.ns.user.response.GlobalResponseHandler;
import com.ns.user.user.dto.request.GrantPermissionRequestDto;
import com.ns.user.user.dto.request.OwnerRegisterRequestDto;
import com.ns.user.user.dto.request.RevokePermissionRequestDto;
import com.ns.user.user.dto.response.PermissionMeResponseDto;
import com.ns.user.user.service.PermissionService;
import com.ns.user.user.vo.*;
import com.ns.user.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
public class PermissionController {
    private final PermissionService permissionService;

    // 노트 생성시에 해당 사용자를 owner로 지정하는 요청
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

    // 노트 권한 부여
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

    // 노트 권한 박탈
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

    // 노트 권한 조회
    @GetMapping("/{noteId}/me")
    public ResponseEntity<GlobalResponseHandler<PermissionMeResponseDto>> permissionMe(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String noteId
    ) {
        PermissionMeQueryVo permissionMeQueryVo = PermissionMeQueryVo.of(noteId, currentUser.id());

        PermissionMeVo permissionMeVo = permissionService.getMyPermission(permissionMeQueryVo);

        PermissionMeResponseDto responseDto =
                PermissionMeResponseDto.of(permissionMeVo.noteId(),
                        permissionMeVo.userId(),
                        permissionMeVo.role().name());

        return GlobalResponseHandler.success(ResponseStatus.PERMISSION_ME_OK, responseDto);
    }}
