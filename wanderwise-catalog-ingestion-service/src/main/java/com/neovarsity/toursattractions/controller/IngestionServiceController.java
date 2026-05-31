package com.neovarsity.toursattractions.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class IngestionServiceController {

    @Value("${server.port:8081}")
    private int port;

    @GetMapping("/api/info")
    public Map<String, Object> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "WanderWise Catalog & Ingestion");
        body.put("port", port);
        body.put("adminPortal", "http://localhost:" + port + "/admin/index.html");
        body.put("customerUi", "http://localhost:8080/");
        return body;
    }
}
