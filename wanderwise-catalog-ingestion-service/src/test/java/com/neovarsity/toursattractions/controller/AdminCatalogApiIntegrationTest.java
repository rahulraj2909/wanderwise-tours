package com.neovarsity.toursattractions.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AdminCatalogApiIntegrationTest {

    private static final String ADMIN_SECRET = "wanderwise-admin";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listProductsRequiresAdminSecret() throws Exception {
        mockMvc.perform(get("/api/v1/admin/catalog/products"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listProductsWithSecretReturnsCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/admin/catalog/products")
                        .param("page", "0")
                        .param("size", "10")
                        .header("X-Admin-Secret", ADMIN_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(greaterThan(0)));
    }
}
