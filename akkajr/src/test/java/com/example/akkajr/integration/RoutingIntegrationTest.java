package com.example.akkajr.integration;

import com.example.akkajr.AkkajrApplication;
import com.example.akkajr.core.WorkerRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour le routage dynamique (PARTIE 4)
 */
@SpringBootTest(classes = AkkajrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoutingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkerRegistry workerRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        // Nettoyer et enregistrer des workers de test
        workerRegistry.getAllWorkers().forEach(w -> 
            workerRegistry.unregister(w.getId())
        );

        // Enregistrer 3 workers pour les tests
        registerWorker("worker-1", "http://localhost:9001", 
            Map.of("region", "us-east", "capability", "payment"));
        registerWorker("worker-2", "http://localhost:9002", 
            Map.of("region", "eu-west", "capability", "payment"));
        registerWorker("worker-3", "http://localhost:9003", 
            Map.of("region", "us-east", "capability", "notification"));
    }

    @Test
    void testRoundRobinSelection() throws Exception {
        mockMvc.perform(post("/api/router/dynamic/roundrobin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.workerId").exists())
                .andExpect(jsonPath("$.workerAddress").exists())
                .andExpect(jsonPath("$.strategy").value("ROUND_ROBIN"));
    }

    @Test
    void testRoundRobinDistribution() throws Exception {
        // Faire plusieurs sélections et vérifier la distribution
        String[] selectedWorkers = new String[6];
        for (int i = 0; i < 6; i++) {
            String response = mockMvc.perform(post("/api/router/dynamic/roundrobin"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            
            Map<String, Object> result = objectMapper.readValue(response, Map.class);
            selectedWorkers[i] = (String) result.get("workerId");
        }

        // Vérifier qu'au moins 2 workers différents ont été sélectionnés
        long distinctWorkers = java.util.Arrays.stream(selectedWorkers)
                .distinct()
                .count();
        assert distinctWorkers >= 2 : "La distribution Round-Robin devrait utiliser plusieurs workers";
    }

    @Test
    void testRoundRobinWithFilter() throws Exception {
        Map<String, Object> filter = new HashMap<>();
        Map<String, String> filterParams = new HashMap<>();
        filterParams.put("tag", "region");
        filterParams.put("value", "us-east");
        filter.put("filter", filterParams);

        mockMvc.perform(post("/api/router/dynamic/roundrobin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.workerId").exists());
    }

    @Test
    void testLoadBalancedSelection() throws Exception {
        mockMvc.perform(post("/api/router/dynamic/loadbalanced"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.workerId").exists())
                .andExpect(jsonPath("$.workerLoad").exists())
                .andExpect(jsonPath("$.strategy").value("LOAD_BALANCED"));
    }

    @Test
    void testLoadBalancedWithFilter() throws Exception {
        Map<String, Object> filter = new HashMap<>();
        Map<String, String> filterParams = new HashMap<>();
        filterParams.put("tag", "capability");
        filterParams.put("value", "payment");
        filter.put("filter", filterParams);

        mockMvc.perform(post("/api/router/dynamic/loadbalanced")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.workerId").exists());
    }

    @Test
    void testDemoStrategies() throws Exception {
        mockMvc.perform(post("/api/router/dynamic/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundRobin").exists())
                .andExpect(jsonPath("$.roundRobin.success").exists())
                .andExpect(jsonPath("$.loadBalanced").exists())
                .andExpect(jsonPath("$.loadBalanced.success").exists());
    }

    @Test
    void testRoundRobinExcludesUnavailableWorkers() throws Exception {
        // Mettre un worker en OFFLINE
        Map<String, String> statusUpdate = new HashMap<>();
        statusUpdate.put("status", "OFFLINE");
        mockMvc.perform(put("/api/workers/worker-1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdate)));

        // Faire plusieurs sélections - worker-1 ne devrait jamais être sélectionné
        for (int i = 0; i < 10; i++) {
            String response = mockMvc.perform(post("/api/router/dynamic/roundrobin"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            
            Map<String, Object> result = objectMapper.readValue(response, Map.class);
            String selectedWorker = (String) result.get("workerId");
            assert !"worker-1".equals(selectedWorker) : 
                "Les workers OFFLINE ne devraient pas être sélectionnés";
        }
    }

    // Helper
    private void registerWorker(String workerId, String address, 
                               Map<String, String> metadata) throws Exception {
        Map<String, Object> worker = new HashMap<>();
        worker.put("workerId", workerId);
        worker.put("address", address);
        worker.put("metadata", metadata);

        mockMvc.perform(post("/api/workers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(worker)));
    }
}

