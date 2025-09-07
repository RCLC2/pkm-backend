package com.ns.user.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 특정 경로는 인증 필터에서 제외
        if(shouldSkipFilter(request)){
            filterChain.doFilter(request,response);
            return;
        }


        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

            String userId = jwtTokenProvider.getUserId(token);
            String email = jwtTokenProvider.getEmail(token);
            String name = jwtTokenProvider.getName(token);
            String role = jwtTokenProvider.getRole(token);

            CurrentUser user = new CurrentUser(userId, email, name, role);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);


    }


    // Bearer 헤더 사용
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private boolean shouldSkipFilter(HttpServletRequest request){
        final List<String> excludedPaths = Arrays.asList(
                // 인증 관련
                "/api/v1/member/**",
                "/api/v1/auth/google/**",
                "/api/v1/auth/signup",
                //문서 모니터링
                "/swagger-ui.html",
                "/swagger-ui",
                "/v3/api-docs"
        );

        String path = request.getRequestURI();

        return excludedPaths.stream()
                .anyMatch(path::startsWith);
    }
}
