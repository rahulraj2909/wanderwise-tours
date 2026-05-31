package com.neovarsity.toursattractions.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FaviconController {

    private static final Resource FAVICON = new ClassPathResource("static/favicon.svg");

    @GetMapping("/favicon.ico")
    public ResponseEntity<Resource> favicon() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(FAVICON);
    }

    @GetMapping("/favicon.svg")
    public ResponseEntity<Resource> faviconSvg() {
        return favicon();
    }
}
