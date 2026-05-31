package com.neovarsity.toursattractions.config;

import com.neovarsity.toursattractions.interceptor.ReviewAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ReviewAuthInterceptor reviewAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(reviewAuthInterceptor).addPathPatterns("/api/v1/reviews");
    }
}
