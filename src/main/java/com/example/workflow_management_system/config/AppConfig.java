package com.example.workflow_management_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<jakarta.servlet.Filter> loggingFilter() {
        org.springframework.boot.web.servlet.FilterRegistrationBean<jakarta.servlet.Filter> registrationBean = new org.springframework.boot.web.servlet.FilterRegistrationBean<>();

        registrationBean.setFilter(new jakarta.servlet.Filter() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response,
                    jakarta.servlet.FilterChain chain)
                    throws java.io.IOException, jakarta.servlet.ServletException {
                jakarta.servlet.http.HttpServletRequest req = (jakarta.servlet.http.HttpServletRequest) request;
                org.slf4j.LoggerFactory.getLogger("RequestLogger").info("RAW_REQUEST: {} {}", req.getMethod(),
                        req.getRequestURI());
                chain.doFilter(request, response);
            }
        });
        registrationBean.setOrder(Integer.MIN_VALUE); // Run FIRST
        return registrationBean;
    }
}
