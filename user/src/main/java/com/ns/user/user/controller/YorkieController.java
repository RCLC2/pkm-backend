package com.ns.user.user.controller;

import com.ns.user.jwt.CurrentUser;
import com.ns.user.jwt.YorkieJwtProvider;
import com.ns.user.response.GlobalResponseHandler;
import com.ns.user.response.ResponseStatus;
import com.ns.user.user.dto.DocumentAttributeDto;
import com.ns.user.user.dto.request.YorkieAuthWebhookRequestDto;
import com.ns.user.user.dto.request.YorkieTokenRequestDto;
import com.ns.user.user.dto.response.YorkieAuthWebhookResponseDto;
import com.ns.user.user.dto.response.YorkieTokenResponseDto;
import com.ns.user.user.service.YorkieService;
import com.ns.user.user.vo.YorkieAuthResultVo;
import com.ns.user.user.vo.YorkieAuthWebhookVo;
import com.ns.user.user.vo.YorkieTokenIssueVo;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/yorkie")
@RequiredArgsConstructor
public class YorkieController {

    private final YorkieService yorkieService;
    private final YorkieJwtProvider yorkieJwtProvider;

    @Value("${yorkie.webhook.secret:${YORKIE_WEBHOOK_SECRET:}}")
    private String webhookSecret;

    // 노트에 접근하기 위한 yorkie 토큰 발급
    @PostMapping("/token")
    public ResponseEntity<GlobalResponseHandler<YorkieTokenResponseDto>> issueToken(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestBody YorkieTokenRequestDto yorkieTokenRequestDto
    ) {
        YorkieTokenIssueVo yorkieTokenIssueVo = YorkieTokenIssueVo.of(yorkieTokenRequestDto.getNoteId(), currentUser.id());

        YorkieTokenResponseDto yorkieTokenResponseDto = yorkieService.issueYorkieToken(yorkieTokenIssueVo);

        return GlobalResponseHandler.success(ResponseStatus.YORKIE_TOKEN_ISSUE, yorkieTokenResponseDto);
    }

    // yorkie 서버에서 호출하는 권한 검증용 webhook 엔드포인트
    @PostMapping("/auth")
    public ResponseEntity<YorkieAuthWebhookResponseDto> authorizeFromYorkie(
//            @RequestHeader(value = "X-Yorkie-Webhook-Secret", required = false) String headerSecret,
            @RequestBody YorkieAuthWebhookRequestDto yorkieAuthWebhookRequestDto
    ) {

//        // 0) Webhook Secret 검증 → 불일치면 401
//        if (StringUtils.hasText(webhookSecret) && !java.util.Objects.equals(webhookSecret, headerSecret)) {
//            return unauth("SECRET_MISMATCH");
//        }

        log.info("==== [WEBHOOK] Yorkie Auth 요청 수신 ====");
        log.info("요청 method={}, token(head)={}, attrs={}",
                yorkieAuthWebhookRequestDto.getMethod(),
                yorkieAuthWebhookRequestDto.getToken() != null
                        ? yorkieAuthWebhookRequestDto.getToken().substring(0, Math.min(20, yorkieAuthWebhookRequestDto.getToken().length())) + "..."
                        : "null",
                yorkieAuthWebhookRequestDto.getDocumentAttributes());


        // 1) documentAttributes: 단일 + key/verb 유효성 체크 → 실패면 403
        DocumentAttributeDto attr = extractSingleAttr(yorkieAuthWebhookRequestDto.getDocumentAttributes());
        if (attr == null) {
            return forbid("INVALID_ATTRIBUTES");
        }

        String noteId = parseNoteId(attr.getKey()); // "note-123" -> "123"
        if (!StringUtils.hasText(noteId) || !StringUtils.hasText(attr.getVerb())) {
            return forbid("INVALID_NOTE_OR_VERB");
        }

        // 2) Yorkie 토큰 서명/만료 검증 → 실패면 401
        YorkieJwtProvider.YorkieClaims claims;
        try {
            claims = yorkieJwtProvider.verifiedYorkieClaims(yorkieAuthWebhookRequestDto.getToken());
        } catch (ExpiredJwtException e) {
            return unauth("token expired");
        } catch (JwtException e) {
            return unauth("invalid token");
        }

        log.info("[WEBHOOK] Parsed noteId={}, verb={}", noteId, attr.getVerb());
        if (!StringUtils.hasText(noteId) || !StringUtils.hasText(attr.getVerb())) {
            log.warn("[WEBHOOK] INVALID_NOTE_OR_VERB key={}, verb={}", attr.getKey(), attr.getVerb());
            return forbid("INVALID_NOTE_OR_VERB");
        }

        // 3) 요청 바디 vs 토큰 claims 교차검증 + 권한 검증을 위해 VO 생성
        YorkieAuthWebhookVo vo = YorkieAuthWebhookVo.of(
                yorkieAuthWebhookRequestDto.getToken(),
                yorkieAuthWebhookRequestDto.getMethod(),
                noteId,
                attr.getVerb()
        );
        // Service 호출(VO 반환)
        YorkieAuthResultVo resultVo = yorkieService.authorizeForAttachYorkie(vo);

        // 4) 결과에 따라 허용/거부 응답 반환
        if (resultVo.allowed()) {
            return ResponseEntity.ok(YorkieAuthWebhookResponseDto.allow());
        } else {
            // 서비스에서 내려준 사유를 reason에 넣어서 403
            return forbid(resultVo.reason());
        }
    }

    // 단일 속성만 허용
    private DocumentAttributeDto extractSingleAttr(List<DocumentAttributeDto> attrs) {
        if (CollectionUtils.isEmpty(attrs) || attrs.size() != 1) return null;
        DocumentAttributeDto a = attrs.getFirst();
        if (!StringUtils.hasText(a.getKey()) || !StringUtils.hasText(a.getVerb())) return null;
        return a;
    }

    // "note-123" -> "123"
    private String parseNoteId(String key) {
        if (!StringUtils.hasText(key)) return null;
        String prefix = "note-";
        return key.startsWith(prefix) ? key.substring(prefix.length()) : null;
    }

    // 401 토큰 관련 로직
    private ResponseEntity<YorkieAuthWebhookResponseDto> unauth(String reason) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(YorkieAuthWebhookResponseDto.deny(reason));
    }

    // 403 권한 관련 로직
    private ResponseEntity<YorkieAuthWebhookResponseDto> forbid(String reason) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(YorkieAuthWebhookResponseDto.deny(reason));
    }
}
