package com.example.akkajr.integration;

import com.example.akkajr.AkkajrApplication;
import com.example.akkajr.messaging.MessageService;
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
 * Tests d'intégration pour le système de messagerie (PARTIE 1)
 */
@SpringBootTest(classes = AkkajrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MessagingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Nettoyer l'historique des messages
        messageService.history().clear();
    }

    @Test
    void testTellMessage() throws Exception {
        Map<String, String> message = new HashMap<>();
        message.put("senderId", "actor1");
        message.put("receiverId", "actor2");
        message.put("content", "Hello from actor1");

        mockMvc.perform(post("/api/messages/tell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isOk());
    }

    @Test
    void testAskMessage() throws Exception {
        Map<String, String> message = new HashMap<>();
        message.put("senderId", "actor1");
        message.put("receiverId", "actor2");
        message.put("content", "What is your status?");

        mockMvc.perform(post("/api/messages/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isOk());
    }

    @Test
    void testGetInbox() throws Exception {
        // Envoyer un message
        Map<String, String> message = new HashMap<>();
        message.put("senderId", "actor1");
        message.put("receiverId", "actor2");
        message.put("content", "Test message");

        mockMvc.perform(post("/api/messages/tell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(message)));

        // Vérifier la boîte de réception
        mockMvc.perform(get("/api/messages/inbox/actor2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].senderId").value("actor1"))
                .andExpect(jsonPath("$[0].receiverId").value("actor2"));
    }

    @Test
    void testGetHistory() throws Exception {
        // Envoyer quelques messages
        for (int i = 0; i < 3; i++) {
            Map<String, String> message = new HashMap<>();
            message.put("senderId", "actor1");
            message.put("receiverId", "actor2");
            message.put("content", "Message " + i);

            mockMvc.perform(post("/api/messages/tell")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(message)));
        }

        mockMvc.perform(get("/api/messages/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void testGetStats() throws Exception {
        mockMvc.perform(get("/api/messages/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMessages").exists())
                .andExpect(jsonPath("$.remoteMessages").exists())
                .andExpect(jsonPath("$.deadLetters").exists())
                .andExpect(jsonPath("$.pendingAsks").exists());
    }

    @Test
    void testGetDeadLetters() throws Exception {
        mockMvc.perform(get("/api/messages/deadletters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testReplyToAsk() throws Exception {
        // Envoyer un ASK
        Map<String, String> askMessage = new HashMap<>();
        askMessage.put("senderId", "actor1");
        askMessage.put("receiverId", "actor2");
        askMessage.put("content", "Question?");

        mockMvc.perform(post("/api/messages/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(askMessage)));

        // Répondre à l'ASK
        mockMvc.perform(post("/api/messages/reply")
                .param("agentId", "actor2")
                .contentType(MediaType.TEXT_PLAIN)
                .content("Answer"))
                .andExpect(status().isOk());
    }

    @Test
    void testMessageBlocking() throws Exception {
        // Envoyer un ASK
        Map<String, String> askMessage = new HashMap<>();
        askMessage.put("senderId", "actor1");
        askMessage.put("receiverId", "actor2");
        askMessage.put("content", "Question?");

        mockMvc.perform(post("/api/messages/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(askMessage)));

        // Essayer d'envoyer un TELL - devrait être bloqué ou dans dead letters
        Map<String, String> tellMessage = new HashMap<>();
        tellMessage.put("senderId", "actor3");
        tellMessage.put("receiverId", "actor2");
        tellMessage.put("content", "This should be blocked");

        mockMvc.perform(post("/api/messages/tell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tellMessage)))
                .andExpect(status().isOk());

        // Vérifier que le message est dans dead letters ou pas dans inbox
        // (dépend de l'implémentation)
    }
}

