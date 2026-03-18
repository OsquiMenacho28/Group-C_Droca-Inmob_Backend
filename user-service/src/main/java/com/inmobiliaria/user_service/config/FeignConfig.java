package com.inmobiliaria.user_service.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userId = request.getHeader("X-Auth-User-Id");
                String roles = request.getHeader("X-Auth-Roles");

                if (userId != null) {
                    requestTemplate.header("X-Auth-User-Id", userId);
                }
                if (roles != null) {
                    requestTemplate.header("X-Auth-Roles", roles);
                }
            }
        };
    }
}
