package com.lolcoaching.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // properties에 적은 'cors.allowed-origin' 값을 가져와서 변수에 담습니다.
    @Value("${cors.allowed-origin}")
    private String allowedOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 로컬 테스트용(localhost)과 실제 서버용(IP) 둘 다 허용해야 합니다.
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // OPTIONS 추가 권장
                .allowedHeaders("*"); // 모든 헤더 허용
    }
}