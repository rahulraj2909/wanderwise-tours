package com.neovarsity.toursattractions.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final Environment environment;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${wanderwise.catalog.base-url:http://localhost:8081}")
    private String catalogBaseUrl;

    public HealthController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        String db = datasourceUrl.contains("mysql") ? "MySQL"
                : datasourceUrl.contains("h2") ? "H2 (file database)"
                : "configured datasource";
        Map<String, String> info = new LinkedHashMap<>();
        info.put("application", "WanderWise Booking Service");
        info.put("status", "UP");
        info.put("database", db);
        info.put("profile", String.join(",", environment.getActiveProfiles()));
        info.put("customerUi", "http://localhost:8080/");
        info.put("catalogService", catalogBaseUrl + "/");
        info.put("swagger", "/swagger-ui.html");
        return info;
    }

    @GetMapping("/api/health/catalog")
    public Map<String, Object> catalogHealth() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("catalogBaseUrl", catalogBaseUrl);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(catalogBaseUrl + "/api/v1/attractions?page=0&size=1"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            info.put("status", response.statusCode() == 200 ? "UP" : "DEGRADED");
            info.put("httpStatus", response.statusCode());
            info.put("hint", response.statusCode() == 200
                    ? "Customer UI at http://localhost:8080/ can load products."
                    : "Start wanderwise-catalog-ingestion-service on port 8081.");
        } catch (Exception ex) {
            info.put("status", "DOWN");
            info.put("error", ex.getMessage());
            info.put("hint", "Run: mvn -pl wanderwise-catalog-ingestion-service spring-boot:run -Dspring-boot.run.profiles=h2");
        }
        return info;
    }
}
