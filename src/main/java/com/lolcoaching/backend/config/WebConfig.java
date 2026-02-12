package com.lolcoaching.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 로컬 테스트용(localhost)과 실제 서버용(IP) 둘 다 허용해야 합니다.
                .allowedOrigins("http://localhost:3000", "http://3.34.82.181:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // OPTIONS 추가 권장
                .allowedHeaders("*"); // 모든 헤더 허용
    }
}