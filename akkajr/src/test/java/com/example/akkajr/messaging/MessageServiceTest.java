package com.example.akkajr.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MessageServiceTest {

    private MessageService messageService;
    
    @Mock
    private RemoteMessageClient remoteMessageClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        messageService = new MessageService();
        // Inject mock RemoteMessageClient
        ReflectionTestUtils.setField(messageService, "remoteMessageClient", remoteMessageClient);
        ReflectionTestUtils.setField(messageService, "currentServiceName", "service1");
        ReflectionTestUtils.setField(messageService, "remoteServiceUrls", 
            Map.of("service2", "http://localhost:8081"));
    }

    @Test
    void testTellLocal() {
        // Arrange
        Message msg = new Message("sender1", "receiver1", "Test message");

        // Act
        messageService.send(msg);

        // Assert
        assertFalse(messageService.history().isEmpty(), "Le message devrait être dans l'historique");
    }

    @Test
    void testAskLocalWithReply() throws Exception {
        // Arrange
        AskMessage ask = new AskMessage("sender1", "receiver1", "Test question");

        // Act
        messageService.send(ask);

        // Vérifier que l'acteur est bloqué
        assertTrue(messageService.isBlocked("receiver1"), "L'acteur devrait être bloqué après réception d'ASK");

        // Simuler une réponse
        messageService.replyToAsk("receiver1", "Test response");

        // Assert
        String response = ask.getFutureResponse().get(5, TimeUnit.SECONDS);
        assertEquals("Test response", response, "La réponse devrait être correcte");
        assertFalse(messageService.isBlocked("receiver1"), "L'acteur ne devrait plus être bloqué");
    }

    @Test
    void testActorBlocking() {
        // Arrange
        AskMessage ask = new AskMessage("sender1", "receiver1", "Test question");
        messageService.send(ask);

        // Assert - Acteur bloqué
        assertTrue(messageService.isBlocked("receiver1"), "L'acteur devrait être bloqué");

        // Act - Envoyer un autre message vers l'acteur bloqué
        Message msg2 = new Message("sender2", "receiver1", "Should be blocked");
        messageService.send(msg2);

        // Assert - Le message devrait être dans dead letters
        assertFalse(messageService.getDeadLetters().isEmpty(), "Le message devrait être dans dead letters");

        // Act - Répondre à l'ASK
        messageService.replyToAsk("receiver1", "Response");

        // Assert - L'acteur ne devrait plus être bloqué
        assertFalse(messageService.isBlocked("receiver1"), "L'acteur ne devrait plus être bloqué");
    }

    @Test
    void testAskTimeout() {
        // Arrange
        AskMessage ask = new AskMessage("sender1", "receiver1", "Test question");
        messageService.send(ask);

        // Act & Assert
        assertThrows(TimeoutException.class, () -> {
            ask.getFutureResponse().get(1, TimeUnit.SECONDS);
        }, "Devrait timeout si aucune réponse n'est donnée");
    }

    @Test
    void testInbox() {
        // Arrange
        Message msg = new Message("sender1", "receiver1", "Test message");
        messageService.send(msg);

        // Act
        var inbox = messageService.inbox("receiver1");

        // Assert
        assertFalse(inbox.isEmpty(), "L'inbox devrait contenir le message");
    }

    @Test
    void testDeadLetters() {
        // Arrange - Envoyer un message vers un service inexistant
        Message msg = new Message("sender1", "unknown-service:receiver1", "Should fail");
        
        // Act
        messageService.send(msg);

        // Assert - Le message devrait être dans dead letters
        assertFalse(messageService.getDeadLetters().isEmpty(), 
            "Le message devrait être dans dead letters pour un service inconnu");
    }

    @Test
    void testPendingAsk() {
        // Arrange
        AskMessage ask = new AskMessage("sender1", "receiver1", "Test question");
        messageService.send(ask);

        // Act
        AskMessage pending = messageService.getPendingAsk("receiver1");

        // Assert
        assertNotNull(pending, "Il devrait y avoir un ASK en attente");
        assertEquals(ask, pending, "L'ASK en attente devrait être le même");
    }

    // NEW TESTS BELOW

    @Test
    void testRemoteTellMessage() {
        // Arrange
        Message msg = new Message("agentA", "service2:agentB", "Remote message");
        
        // Act
        messageService.send(msg);
        
        // Assert
        verify(remoteMessageClient, times(1)).sendTell(any(Message.class), eq("http://localhost:8081"));
    }

    @Test
    void testRemoteAskMessage() {
        // Arrange
        AskMessage ask = new AskMessage("agentA", "service2:agentB", "Remote question?");
        CompletableFuture<String> future = CompletableFuture.completedFuture("Answer");
        
        when(remoteMessageClient.sendAsk(any(AskMessage.class), anyString()))
            .thenReturn(future);
        
        // Act
        messageService.send(ask);
        
        // Assert
        verify(remoteMessageClient, times(1)).sendAsk(any(AskMessage.class), eq("http://localhost:8081"));
    }

    @Test
    void testUnknownRemoteService() {
        // Arrange
        Message msg = new Message("agentA", "unknown-service:agentB", "Should fail");
        
        // Act
        messageService.send(msg);
        
        // Assert
        assertFalse(messageService.getDeadLetters().isEmpty(), 
            "Message should be in dead letters for unknown service");
        verify(remoteMessageClient, never()).sendTell(any(), anyString());
    }

    @Test
    void testMultipleAsksToSameAgent() {
        // Arrange
        AskMessage ask1 = new AskMessage("agentA", "agentB", "First question");
        AskMessage ask2 = new AskMessage("agentC", "agentB", "Second question");
        
        // Act
        messageService.send(ask1);
        messageService.send(ask2);
        
        // Assert
        assertTrue(messageService.isBlocked("agentB"));
        assertFalse(messageService.getDeadLetters().isEmpty(), 
            "Second ASK should be in dead letters");
        assertEquals(ask1, messageService.getPendingAsk("agentB"), 
            "First ASK should be pending");
    }

    @Test
    void testInboxForRemoteAgent() {
        // Arrange
        String remoteAgent = "service2:agentB";
        
        // Act
        var inbox = messageService.inbox(remoteAgent);
        
        // Assert
        assertTrue(inbox.isEmpty(), 
            "Remote agent inbox should be empty (can't access directly)");
    }

    @Test
    void testHistoryTracksAllMessages() {
        // Arrange
        Message msg1 = new Message("agentA", "agentB", "Message 1");
        Message msg2 = new Message("agentC", "agentD", "Message 2");
        
        // Act
        messageService.send(msg1);
        messageService.send(msg2);
        
        // Assert
        assertEquals(2, messageService.history().size(), 
            "History should contain all messages");
    }

    @Test
    void testReplyToNonExistentAsk() {
        // Act
        messageService.replyToAsk("agentB", "Answer");
        
        // Assert - Should not throw, just log warning
        assertFalse(messageService.isBlocked("agentB"));
    }

    @Test
    void testOriginServiceTracking() {
        // Arrange
        Message msg = new Message("agentA", "agentB", "Test");
        msg.setOriginService("service1");
        
        // Act
        messageService.send(msg);
        
        // Assert
        var history = messageService.history();
        Message logged = history.stream()
            .filter(m -> m.getContent().equals("Test"))
            .findFirst()
            .orElse(null);
        assertNotNull(logged);
        assertEquals("service1", logged.getOriginService());
    }
}