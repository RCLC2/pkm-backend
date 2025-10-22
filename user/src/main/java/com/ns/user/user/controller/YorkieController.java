package com.ns.user.user.controller;

import com.ns.user.jwt.CurrentUser;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/yorkie")
@RequiredArgsConstructor
public class YorkieController {

    private final YorkieService yorkieService;


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
        @RequestBody YorkieAuthWebhookRequestDto yorkieAuthWebhookRequestDto) {  
      
    
    if ("ActivateClient".equals(yorkieAuthWebhookRequestDto.getMethod())) {  
        YorkieAuthWebhookResponseDto response = YorkieAuthWebhookResponseDto.allow();  
        return ResponseEntity.ok(response);  
    }  

    // attributes 검증  
    DocumentAttributeDto attr = extractSingleAttr(yorkieAuthWebhookRequestDto.getAttributes());  
    if (attr == null) {  
        YorkieAuthWebhookResponseDto response = YorkieAuthWebhookResponseDto.deny("INVALID_ATTRIBUTES");  
        return ResponseEntity.ok(response);  
    }  
   
    String noteId = parseNoteId(attr.getKey());  
    if (!StringUtils.hasText(noteId) || !StringUtils.hasText(attr.getVerb())) {  
        YorkieAuthWebhookResponseDto response = YorkieAuthWebhookResponseDto.deny("INVALID_NOTE_OR_VERB");  
        return ResponseEntity.ok(response);  
    }  
   
    YorkieAuthWebhookVo vo = YorkieAuthWebhookVo.of(  
            yorkieAuthWebhookRequestDto.getToken(),  
            yorkieAuthWebhookRequestDto.getMethod(),  
            noteId,  
            attr.getVerb()  
    );      
    YorkieAuthResultVo resultVo = yorkieService.authorizeForAttachYorkie(vo);  

    return ResponseEntity.ok(
        resultVo.allowed() 
                    ? YorkieAuthWebhookResponseDto.allow() 
                    : YorkieAuthWebhookResponseDto.deny(resultVo.reason())
                    );  
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
}
