package com.ns.gateway.utils;

public class AuthErrorMessages {
    public static final String NO_AUTH_HEADER = "Authorization 헤더가 없습니다.";
    public static final String INVALID_TOKEN_FORMAT = "유효하지 않은 JWT 토큰 형식입니다. 'Bearer '로 시작해야 합니다.";
    public static final String INVALID_SIGNATURE = "유효하지 않은 JWT 서명입니다.";
    public static final String EXPIRED_TOKEN = "만료된 JWT 토큰입니다.";
    public static final String UNSUPPORTED_TOKEN = "지원되지 않는 JWT 토큰입니다.";
    public static final String MALFORMED_TOKEN = "손상된 JWT 토큰입니다.";
    public static final String EMPTY_CLAIMS = "JWT 클레임 문자열이 비어 있습니다.";
    public static final String MISSING_USER_ID = "유효하지 않은 JWT: userId 클레임이 누락되었습니다.";
    public static final String AUTHENTICATION_FAILED = "인증에 실패했습니다. 내부 서버 오류.";
}
