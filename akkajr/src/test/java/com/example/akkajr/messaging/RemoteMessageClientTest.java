package com.example.akkajr.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class RemoteMessageClientTest {

    private RemoteMessageClient remoteMessageClient;

    @BeforeEach
    void setUp() {
        remoteMessageClient = new RemoteMessageClient();
    }

    @Test
    void testSendTellWithValidUrl() {
        // Arrange
        Message msg = new Message("agentA", "agentB", "Test");
        String baseUrl = "http://localhost:8081";
        
        // Act - Should not throw
        assertDoesNotThrow(() -> {
            remoteMessageClient.sendTell(msg, baseUrl);
        });
    }

    @Test
    void testSendAskWithValidUrl() {
        // Arrange
        AskMessage ask = new AskMessage("agentA", "agentB", "Question?");
        String baseUrl = "http://localhost:8081";
        
        // Act
        CompletableFuture<String> future = remoteMessageClient.sendAsk(ask, baseUrl);
        
        // Assert
        assertNotNull(future);
        // Note: In a real test, you'd mock WebClient to return a response
    }

    @Test
    void testSendTellWithNullMessage() {
        // Arrange
        String baseUrl = "http://localhost:8081";
        
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            remoteMessageClient.sendTell(null, baseUrl);
        });
    }

    @Test
    void testSendAskWithNullMessage() {
        // Arrange
        String baseUrl = "http://localhost:8081";
        
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            remoteMessageClient.sendAsk(null, baseUrl);
        });
    }
}