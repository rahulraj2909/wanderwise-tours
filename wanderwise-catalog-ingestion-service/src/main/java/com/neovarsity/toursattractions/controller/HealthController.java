package com.neovarsity.toursattractions.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final Environment environment;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    public HealthController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        String db = datasourceUrl.contains("mysql") ? "MySQL"
                : datasourceUrl.contains("h2") ? "H2 (file database)"
                : "configured datasource";
        Map<String, String> info = new LinkedHashMap<>();
        info.put("application", "Tours & Attractions Platform");
        info.put("status", "UP");
        info.put("database", db);
        info.put("profile", String.join(",", environment.getActiveProfiles()));
        info.put("ui", "/");
        info.put("swagger", "/swagger-ui.html");
        return info;
    }
}
