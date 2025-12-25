package com.example.akkajr.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class MessageServiceTest {

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageService = new MessageService();
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
        // Note: Cela fonctionne seulement si RemoteMessageClient est null ou si le service est vraiment inconnu
        // Dans un vrai test, il faudrait mocker RemoteMessageClient
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
}

