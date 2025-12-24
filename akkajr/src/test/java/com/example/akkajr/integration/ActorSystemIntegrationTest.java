package com.example.akkajr.integration;

import com.example.akkajr.AkkajrApplication;
import com.example.akkajr.core.actors.ActorSystem;
import com.example.akkajr.core.actors.Props;
import com.example.akkajr.core.actors.SupervisorActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour le système d'acteurs (PARTIE 1)
 */
@SpringBootTest(classes = AkkajrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActorSystemIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("coreActorSystem")
    private ActorSystem actorSystem;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testInitializeActors() throws Exception {
        mockMvc.perform(post("/api/actors/init"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.services").isArray());
    }

    @Test
    void testCreateOrder() throws Exception {
        // Initialiser les acteurs d'abord
        mockMvc.perform(post("/api/actors/init"));

        // Créer une commande
        Map<String, Object> order = new HashMap<>();
        order.put("items", List.of("Phone", "Tablet"));

        mockMvc.perform(post("/api/actors/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk());
    }

    @Test
    void testListActors() throws Exception {
        // Initialiser les acteurs
        mockMvc.perform(post("/api/actors/init"));

        // Lister les acteurs
        mockMvc.perform(get("/api/actors/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void testActorCreation() {
        // Test direct de création d'acteur
        var props = Props.create(SupervisorActor.class);
        var actorRef = actorSystem.actorOf(props, "test-actor");

        assertNotNull(actorRef, "L'acteur devrait être créé");
        assertNotNull(actorRef.path(), "L'acteur devrait avoir un path");
    }

    @Test
    void testActorSystemMetrics() {
        // Vérifier que le système d'acteurs est initialisé
        assertNotNull(actorSystem, "L'ActorSystem devrait être initialisé");

        // Vérifier les métriques
        var snapshot = actorSystem.metricsSnapshot();
        assertNotNull(snapshot, "Les métriques devraient être disponibles");
    }
}

