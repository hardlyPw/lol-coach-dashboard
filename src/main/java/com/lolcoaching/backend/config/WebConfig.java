package com.lolcoaching.backend.config; // 본인의 패키지 경로에 맞게 수정하세요

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origin}")
    private String allowedOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // ★ [로그 추가] 서버 터미널에서 이 부분을 확인하세요!
        System.out.println("===============================================");
        System.out.println("CORS 설정 로드 중...");
        System.out.println("현재 허용된 Origin: " + allowedOrigin);
        System.out.println("===============================================");

        registry.addMapping("/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}