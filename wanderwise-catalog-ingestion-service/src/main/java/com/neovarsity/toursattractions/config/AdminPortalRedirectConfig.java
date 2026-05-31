package com.neovarsity.toursattractions.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdminPortalRedirectConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/admin/index.html");
        registry.addRedirectViewController("/admin", "/admin/index.html");
        registry.addViewController("/admin/").setViewName("forward:/admin/index.html");
    }
}
