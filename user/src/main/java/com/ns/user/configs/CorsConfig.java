package com.ns.user.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 허용 주소
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",   // 로컬 개발 환경
                "http://127.0.0.1:3000"    // 혹시 다른 호스트명으로 접근할 경우 대비
        ));
        // 허용 메소드
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 허용 헤더
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));

        // 클라이언트가 Authorization 헤더를 보낼 수 있게 함
        config.setAllowCredentials(true);

        // preflight 캐시
        config.setMaxAge(3600L);

        // 설정을 전체 엔드포인트에 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
