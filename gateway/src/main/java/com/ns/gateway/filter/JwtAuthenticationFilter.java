package com.ns.gateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.ns.gateway.utils.AuthErrorMessages.*;
import static com.ns.gateway.utils.AuthLogMessages.*;

@Slf4j
@Component("JwtAuthentication")
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {
    private final SecretKey signingKey;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret) {
        super(Config.class);
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /* {
      "userId": "testuser123",
      "roles": ["ROLE_USER", "ROLE_ADMIN"],
      "exp": 1752307200,
      "iat": 1718006400
    } */

    @Getter
    @Setter
    public static class Config {
        private List<String> publicPaths;  // 인증이 필요 없는 경로 (로그인, 회원가입, health check...)
    }

    @Override
    public GatewayFilter apply(Config config) {
        List<String> currentPublicPaths = config.getPublicPaths() != null ? config.getPublicPaths() : Collections.emptyList();

        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (isPublicPath(request.getURI().getPath(), currentPublicPaths)) {
                log.info(PUBLIC_PATH_SKIPPED, request.getURI().getPath());
                return chain.filter(exchange);
            }

            String jwt;
            try {
                jwt = extractJwtToken(request);
            } catch (IllegalArgumentException e) {
                return onError(exchange, e.getMessage(), HttpStatus.UNAUTHORIZED);
            }

            try {
                Claims claims = validateJwt(jwt);
                ServerHttpRequest mutatedRequest = retainHeader(request, claims);

                String userId = claims.get("userId", String.class);
                List<String> roles = extractRoles(claims);

                log.info(AUTH_SUCCESS, userId, String.join(",", roles));
                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (SignatureException e) {
                log.error("{} - {}", INVALID_SIGNATURE, e.getMessage());
                return onError(exchange, INVALID_SIGNATURE, HttpStatus.UNAUTHORIZED);
            } catch (ExpiredJwtException e) {
                log.error("{} - {}", EXPIRED_TOKEN, e.getMessage());
                return onError(exchange, EXPIRED_TOKEN, HttpStatus.UNAUTHORIZED);
            } catch (UnsupportedJwtException e) {
                log.error("{} - {}", UNSUPPORTED_TOKEN, e.getMessage());
                return onError(exchange, UNSUPPORTED_TOKEN, HttpStatus.UNAUTHORIZED);
            } catch (MalformedJwtException e) {
                log.error("{} - {}", MALFORMED_TOKEN, e.getMessage());
                return onError(exchange, MALFORMED_TOKEN, HttpStatus.UNAUTHORIZED);
            } catch (IllegalArgumentException e) {
                log.error("{} - {}", EMPTY_CLAIMS, e.getMessage());
                return onError(exchange, EMPTY_CLAIMS, HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                log.error("{} - {}", AUTHENTICATION_FAILED, e.getMessage(), e);
                return onError(exchange, AUTHENTICATION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private String extractJwtToken(ServerHttpRequest request) {
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            throw new IllegalArgumentException(NO_AUTH_HEADER);
        }

        String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException(INVALID_TOKEN_FORMAT);
        }

        return authorizationHeader.substring("Bearer ".length());
    }

    private Claims validateJwt(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }


    /** 마이크로 서비스들은 헤더의 이 정보들만 가지고 사용자를 판단 */
    private ServerHttpRequest retainHeader(ServerHttpRequest request, Claims claims) {
        String userId = claims.get("userId", String.class);
        List<String> roles = extractRoles(claims);

        if (userId == null) {
            throw new IllegalArgumentException(MISSING_USER_ID);
        }

        String originalAuthHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        return request.mutate()
                .header("X-User-ID", userId)
                .header("X-User-Roles", String.join(",", roles))
                .header("Authorization", originalAuthHeader)
                .build();
    }

    private List<String> extractRoles(Claims claims) {
        List<String> roles = new ArrayList<>();
        Object rolesObject = claims.get("roles");
        if (rolesObject instanceof List) {
            roles = ((List<?>) rolesObject).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toList());
        } else if (rolesObject instanceof String) {
            roles = List.of(((String) rolesObject).split(","));
        }
        return roles;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String errorJson = String.format(
                "{\"timestamp\": \"%s\", \"status\": %d, \"error\": \"%s\", \"path\": \"%s\"}",
                Instant.now().toString(),
                httpStatus.value(),
                err,
                exchange.getRequest().getPath()
        );

        log.error(AUTH_ERROR, err, exchange.getRequest().getPath());
        return response.writeWith(Mono.just(response.bufferFactory().wrap(errorJson.getBytes(StandardCharsets.UTF_8))));
    }

    private boolean isPublicPath(String path, List<String> publicPaths) {
        return publicPaths.stream().anyMatch(path::startsWith);
    }
}