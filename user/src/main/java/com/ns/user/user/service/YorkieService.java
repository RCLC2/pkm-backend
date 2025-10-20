package com.ns.user.user.service;

import com.ns.user.exception.ServiceException;
import com.ns.user.jwt.YorkieJwtProvider;
import com.ns.user.user.dto.DocumentAttributeDto;
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
        // PermissionRole role = permissionService.roleOf(yorkieTokenIssueVo.noteId(), yorkieTokenIssueVo.requesterUserId());
        // if (role == null) {
        //     throw new ServiceException(PERMISSION_NOT_FOUND);
        // }
        PermissionRole role = PermissionRole.WRITER;
        String verb = roleToVerb(role); // OWNER/WRITER -> "rw", READER -> "r"

        String token = yorkieJwtProvider.generateYorkieToken(
                yorkieTokenIssueVo.requesterUserId(),
                yorkieTokenIssueVo.noteId(),
                role.name(),
                verb,
                Duration.ofSeconds(yorkieTtlSeconds)
        );


        return new YorkieTokenResponseDto(
                token,
                yorkieTtlSeconds,
                new DocumentAttributeDto("note-" + yorkieTokenIssueVo.noteId(), verb)
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
            if (!"rw".equals(yorkieClaims.getVerb()) || !"r".equals(yorkieAuthWebhookVo.verb())) {  
                return YorkieAuthResultVo.of(false, "VERB_MISMATCH"); // rw는 읽기도 포함해야하니꼐..
            }  
        }

        String method = yorkieAuthWebhookVo.method();
        String verb = yorkieClaims.getVerb();

        // 권한 검증 (AttachDocument, WatchDocuments: 무조건 허용, PushPull: verb에 따라 rw만 허용)
        return switch (method) {
            case "AttachDocument", "WatchDocuments" -> YorkieAuthResultVo.of(true, null);
            case "PushPull" -> {
                if (!"rw".equals(verb)) {
                    yield YorkieAuthResultVo.of(false, "READ_ONLY");
                }
                yield YorkieAuthResultVo.of(true, null);
            }

            // (선택) 기타 서버 호출
            case "ActivateClient" -> YorkieAuthResultVo.of(true, null);
            default -> YorkieAuthResultVo.of(false, "METHOD_NOT_ALLOWED");
        };

    }

    private String roleToVerb(PermissionRole role) {
        return switch (role) {
            case OWNER, WRITER -> "rw";
            case READER -> "r";
        };
    }

}
