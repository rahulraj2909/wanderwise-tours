package com.neovarsity.toursattractions.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AuthApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void demoLoginSucceeds() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/v1/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"customer@tours.demo","password":"demo123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("customer@tours.demo"));

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("customer@tours.demo"));
    }
}
