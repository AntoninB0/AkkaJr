package com.example.akkajr.messaging;
import com.example.akkajr.messaging.mailbox.DeadLetterMailbox;
import com.example.akkajr.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeadLetterMailboxTest {

    private DeadLetterMailbox deadLetters;

    @BeforeEach
    void setUp() {
        deadLetters = new DeadLetterMailbox();
    }

    @Test
    void testPushAndGetAll() {
        // Arrange
        Message msg1 = new Message("agentA", "agentB", "Blocked message");
        Message msg2 = new Message("agentC", "agentD", "Failed message");
        
        // Act
        deadLetters.push(msg1);
        deadLetters.push(msg2);
        
        // Assert
        assertEquals(2, deadLetters.size());
        assertFalse(deadLetters.isEmpty());
        assertEquals(2, deadLetters.getAll().size());
    }

    @Test
    void testEmptyDeadLetters() {
        // Assert
        assertTrue(deadLetters.isEmpty());
        assertEquals(0, deadLetters.size());
    }

    @Test
    void testGetAllReturnsCopy() {
        // Arrange
        Message msg = new Message("agentA", "agentB", "Test");
        deadLetters.push(msg);
        
        // Act
        var copy1 = deadLetters.getAll();
        var copy2 = deadLetters.getAll();
        
        // Assert
        assertNotSame(copy1, copy2, "Should return new copy each time");
        assertEquals(1, copy1.size());
        assertEquals(1, copy2.size());
    }
}