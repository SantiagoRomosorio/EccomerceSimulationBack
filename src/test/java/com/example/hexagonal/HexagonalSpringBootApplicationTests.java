package com.example.hexagonal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HexagonalSpringBootApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void healthReturnsStandardApiResponse() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.developerMessage").doesNotExist())
                .andExpect(jsonPath("$.message").value("Service is healthy"))
                .andExpect(jsonPath("$.path").value("/api/health"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("hexagonal-spring-boot"));
    }

    @Test
    void swaggerDocumentsStandardResponsesByEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/health'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/health'].get.responses['500']").exists());
    }
}
