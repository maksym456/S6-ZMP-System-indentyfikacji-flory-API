package com.ezielnik.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AccountStatusInterceptor accountStatusInterceptor;

    public WebConfig(AccountStatusInterceptor accountStatusInterceptor) {
        this.accountStatusInterceptor = accountStatusInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accountStatusInterceptor)
                .addPathPatterns("/**");
    }
}