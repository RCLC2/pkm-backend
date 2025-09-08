package com.ns.user.oauth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import com.ns.user.exception.AuthException;
import com.ns.user.exception.ExceptionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;


@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleIdTokenVerifier {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.oauth.issuer}")
    // id_token(JWT)의 iss claim이 진짜 구글이 발급한 건지 확인하는 기준
    private String issuer;

    @Value("${google.oauth.jwks-uri}")
    // 구글의 RSA 공개키 -> id_token을 검증
    private String jwksUri;


    // Google ID Token 검증 및 사용자 정보 추출
    public GoogleUserInfo verify(String idToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(idToken);

            // iss(구글에서 발급한지 검증), aud(이 토큰이 우리 앱을 위한건지 검증)
            String tokenIssuer = signedJWT.getJWTClaimsSet().getIssuer();
            String tokenAudience = signedJWT.getJWTClaimsSet().getAudience().get(0);

            if (!issuer.equals(tokenIssuer) || !clientId.equals(tokenAudience)) {
                throw new AuthException(ExceptionStatus.AUTH_GOOGLE_TOKEN_INVALID);
            }

            // JWK 세트 가져오기 (구글 공개키)
            JWKSet jwkSet = JWKSet.load(new URL(jwksUri));
            // 서명 검증
            JWK jwk = jwkSet.getKeyByKeyId(signedJWT.getHeader().getKeyID());

            if (jwk == null) {
                throw new AuthException(ExceptionStatus.AUTH_GOOGLE_TOKEN_INVALID);
            }

            // 공개키로 서명 검증
            boolean valid = signedJWT.verify(new RSASSAVerifier(jwk.toRSAKey()));
            if (!valid) {
                throw new AuthException(ExceptionStatus.AUTH_GOOGLE_TOKEN_INVALID);
            }

            // 만료 검증
            if (signedJWT.getJWTClaimsSet().getExpirationTime().before(new Date())) {
                throw new AuthException(ExceptionStatus.AUTH_GOOGLE_TOKEN_EXPIRED);
            }

            // sub, email, name 추출
            String sub = signedJWT.getJWTClaimsSet().getSubject();
            String email = signedJWT.getJWTClaimsSet().getStringClaim("email");
            String name = signedJWT.getJWTClaimsSet().getStringClaim("name");

            return new GoogleUserInfo(sub, email, name);

        } catch (ParseException | MalformedURLException | JOSEException e) {
            log.error("ID Token 검증 실패", e);
            throw new AuthException(ExceptionStatus.AUTH_GOOGLE_ID_TOKEN_PARSE_ERROR);
        } catch (IOException e) {
            throw new AuthException(ExceptionStatus.AUTH_GOOGLE_JWKS_UNAVAILABLE);
        }
    }

    // 구글 사용자 정보 객체
    public record GoogleUserInfo(
            String sub,    // Google 고유 ID
            String email,
            String name
    ) {}
}

