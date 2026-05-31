package com.neovarsity.toursattractions.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI toursOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tours & Attractions API")
                        .description("Catalog and vendor ingestion API (port 8081) — listings, admin, and vendor feeds")
                        .version("1.0.0"));
    }
}
