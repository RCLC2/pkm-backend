package com.ns.user.user.service;

import com.ns.user.exception.ServiceException;
import com.ns.user.jwt.YorkieJwtProvider;
import com.ns.user.user.dto.DocumentAttributeDto;
import com.ns.user.user.dto.response.YorkieAuthWebhookResponseDto;
import com.ns.user.user.dto.response.YorkieTokenResponseDto;
import com.ns.user.user.entity.PermissionRole;
import com.ns.user.user.vo.YorkieAuthResultVo;
import com.ns.user.user.vo.YorkieAuthWebhookVo;
import com.ns.user.user.vo.YorkieTokenIssueVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.ns.user.exception.ExceptionStatus.PERMISSION_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class YorkieService {

    private final PermissionService permissionService;
    private final YorkieJwtProvider yorkieJwtProvider; // Yorkie 전용 JWT 서명/검증

    @Value("${yorkie.jwt.ttl-seconds:600}")
    private int yorkieTtlSeconds; // 기본 10분

    public YorkieTokenResponseDto issueYorkieToken(YorkieTokenIssueVo yorkieTokenIssueVo) {
        PermissionRole role = permissionService.roleOf(yorkieTokenIssueVo.noteId(), yorkieTokenIssueVo.requesterUserId());
        if (role == null) {
            throw new ServiceException(PERMISSION_NOT_FOUND);
        }

        String verb = roleToVerb(role); // OWNER/WRITER -> "rw", READER -> "r"

        String token = yorkieJwtProvider.generateYorkieToken(
                yorkieTokenIssueVo.requesterUserId(),
                yorkieTokenIssueVo.noteId(),
                role.name(),
                verb,
                "PushPull",
                Duration.ofSeconds(yorkieTtlSeconds)
        );


        return new YorkieTokenResponseDto(
                token,
                yorkieTtlSeconds,
                new DocumentAttributeDto("note:" + yorkieTokenIssueVo.noteId(), verb)
        );
    }

    public YorkieAuthResultVo authorizeForAttachYorkie(YorkieAuthWebhookVo yorkieAuthWebhookVo) {

        // 토큰 값으로 claims 값 추출
        YorkieJwtProvider.YorkieClaims yorkieClaims = yorkieJwtProvider.verifiedYorkieClaims(
                yorkieAuthWebhookVo.token()
        );

        //  noteId/verb 교차검증 (토큰 vs 요청 바디)

        if (!yorkieAuthWebhookVo.noteId().equals(yorkieClaims.getNoteId())) {
            return YorkieAuthResultVo.of(false, "NOTE_ID_MISMATCH");
        }
        if (!yorkieAuthWebhookVo.verb().equals(yorkieClaims.getVerb())) {
            return YorkieAuthResultVo.of(false, "VERB_MISMATCH");
        }

        return YorkieAuthResultVo.of(true, null);
    }


    private String roleToVerb(PermissionRole role) {
        return switch (role) {
            case OWNER, WRITER -> "rw";
            case READER -> "r";
        };
    }

}
