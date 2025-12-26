package com.example.akkajr.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

class AskMessageTest {

    @Test
    void testAskMessageCreation() {
        // Arrange & Act
        AskMessage ask = new AskMessage("sender1", "receiver1", "Question?");
        
        // Assert
        assertEquals("sender1", ask.getSenderId());
        assertEquals("receiver1", ask.getReceiverId());
        assertEquals("Question?", ask.getContent());
        assertNotNull(ask.getFutureResponse());
        assertFalse(ask.getFutureResponse().isDone());
    }

    @Test
    void testAskMessageComplete() throws Exception {
        // Arrange
        AskMessage ask = new AskMessage("sender1", "receiver1", "Question?");
        
        // Act
        ask.complete("Answer");
        
        // Assert
        assertTrue(ask.getFutureResponse().isDone());
        assertEquals("Answer", ask.getFutureResponse().get());
    }

    @Test
    void testAskMessageCompleteExceptionally() {
        // Arrange
        AskMessage ask = new AskMessage("sender1", "receiver1", "Question?");
        RuntimeException error = new RuntimeException("Error");
        
        // Act
        ask.getFutureResponse().completeExceptionally(error);
        
        // Assert
        assertTrue(ask.getFutureResponse().isDone());
        assertTrue(ask.getFutureResponse().isCompletedExceptionally());
        assertThrows(Exception.class, () -> ask.getFutureResponse().get());
    }

    @Test
    void testAskMessageTimeout() {
        // Arrange
        AskMessage ask = new AskMessage("sender1", "receiver1", "Question?");
        
        // Act & Assert
        assertThrows(TimeoutException.class, () -> {
            ask.getFutureResponse().get(100, TimeUnit.MILLISECONDS);
        });
    }
}