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
class AttractionApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void searchReturnsSeededProducts() throws Exception {
        mockMvc.perform(get("/api/v1/attractions")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "popularity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.totalElements").value(greaterThan(0)));
    }

    @Test
    void getByIdReturnsProduct() throws Exception {
        mockMvc.perform(get("/api/v1/attractions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").exists());
    }

    @Test
    void listPaxTypesForProduct() throws Exception {
        mockMvc.perform(get("/api/v1/attractions/1/pax-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThan(0)));
    }
}
