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
 * Tests d'intégration pour la gestion des workers (PARTIE 2)
 */
@SpringBootTest(classes = AkkajrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkerManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkerRegistry workerRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Nettoyer le registre avant chaque test
        workerRegistry.getAllWorkers().forEach(w -> 
            workerRegistry.unregister(w.getId())
        );
    }

    @Test
    void testRegisterWorker() throws Exception {
        Map<String, Object> worker = new HashMap<>();
        worker.put("workerId", "worker-1");
        worker.put("address", "http://localhost:9001");
        worker.put("metadata", new HashMap<>());

        mockMvc.perform(post("/api/workers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(worker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("registered"))
                .andExpect(jsonPath("$.workerId").value("worker-1"));
    }

    @Test
    void testRegisterWorkerWithMetadata() throws Exception {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("region", "eu-west-1");
        metadata.put("capability", "payment");
        metadata.put("tier", "premium");

        Map<String, Object> worker = new HashMap<>();
        worker.put("workerId", "worker-payment");
        worker.put("address", "http://localhost:9002");
        worker.put("metadata", metadata);

        mockMvc.perform(post("/api/workers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(worker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerId").value("worker-payment"));
    }

    @Test
    void testGetAllWorkers() throws Exception {
        // Enregistrer quelques workers
        registerWorker("worker-1", "http://localhost:9001");
        registerWorker("worker-2", "http://localhost:9002");

        mockMvc.perform(get("/api/workers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testGetWorkerById() throws Exception {
        registerWorker("worker-1", "http://localhost:9001");

        mockMvc.perform(get("/api/workers/worker-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("worker-1"))
                .andExpect(jsonPath("$.address").value("http://localhost:9001"));
    }

    @Test
    void testUpdateWorkerStatus() throws Exception {
        registerWorker("worker-1", "http://localhost:9001");

        Map<String, String> statusUpdate = new HashMap<>();
        statusUpdate.put("status", "BUSY");

        mockMvc.perform(put("/api/workers/worker-1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("updated"))
                .andExpect(jsonPath("$.newStatus").value("BUSY"));
    }

    @Test
    void testDeleteWorker() throws Exception {
        registerWorker("worker-1", "http://localhost:9001");

        mockMvc.perform(delete("/api/workers/worker-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("unregistered"));

        // Vérifier que le worker n'existe plus
        mockMvc.perform(get("/api/workers/worker-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSearchWorkersByTag() throws Exception {
        // Enregistrer workers avec différents tags
        registerWorkerWithMetadata("worker-1", "http://localhost:9001", 
            Map.of("region", "us-east"));
        registerWorkerWithMetadata("worker-2", "http://localhost:9002", 
            Map.of("region", "eu-west"));
        registerWorkerWithMetadata("worker-3", "http://localhost:9003", 
            Map.of("region", "us-east"));

        mockMvc.perform(get("/api/workers/search")
                .param("tag", "region")
                .param("value", "us-east"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testGetAvailableWorkers() throws Exception {
        registerWorker("worker-1", "http://localhost:9001");
        registerWorker("worker-2", "http://localhost:9002");

        // Mettre worker-2 en BUSY
        Map<String, String> statusUpdate = new HashMap<>();
        statusUpdate.put("status", "BUSY");
        mockMvc.perform(put("/api/workers/worker-2/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdate)));

        // Seuls les workers AVAILABLE devraient être retournés
        mockMvc.perform(get("/api/workers/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("worker-1"));
    }

    @Test
    void testHeartbeat() throws Exception {
        registerWorker("worker-1", "http://localhost:9001");

        mockMvc.perform(post("/api/workers/worker-1/heartbeat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    // Helpers
    private void registerWorker(String workerId, String address) throws Exception {
        Map<String, Object> worker = new HashMap<>();
        worker.put("workerId", workerId);
        worker.put("address", address);
        worker.put("metadata", new HashMap<>());

        mockMvc.perform(post("/api/workers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(worker)));
    }

    private void registerWorkerWithMetadata(String workerId, String address, 
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

