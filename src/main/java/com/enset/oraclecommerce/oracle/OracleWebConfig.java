package com.enset.oraclecommerce.oracle;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OracleWebConfig implements WebMvcConfigurer {

    private final OracleTraceInterceptor interceptor;

    public OracleWebConfig(OracleTraceInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/oracle", "/api/oracle/**", "/sql", "/api/sql/**",
                        "/css/**", "/js/**", "/images/**", "/error/**", "/favicon.ico");
    }
}
