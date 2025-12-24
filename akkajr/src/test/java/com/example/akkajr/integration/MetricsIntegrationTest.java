package com.example.akkajr.integration;

import com.example.akkajr.AkkajrApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour les métriques et l'observabilité
 */
@SpringBootTest(classes = AkkajrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MetricsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetActorsMetrics() throws Exception {
        mockMvc.perform(get("/api/metrics/actors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void testGetActorsDetail() throws Exception {
        mockMvc.perform(get("/api/metrics/actors/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetEvents() throws Exception {
        mockMvc.perform(get("/api/metrics/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetAlerts() throws Exception {
        mockMvc.perform(get("/api/metrics/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetMetricsStream() throws Exception {
        mockMvc.perform(get("/api/metrics/stream"))
                .andExpect(status().isOk());
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void testPrometheusEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }
}

