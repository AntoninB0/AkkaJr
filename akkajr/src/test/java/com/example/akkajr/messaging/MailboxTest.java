package com.example.akkajr.messaging;

import com.example.akkajr.messaging.Message;
import com.example.akkajr.messaging.mailbox.Mailbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MailboxTest {

    private Mailbox mailbox;

    @BeforeEach
    void setUp() {
        mailbox = new Mailbox();
    }

    @Test
    void testEnqueueAndSize() {
        // Arrange
        Message msg1 = new Message("agentA", "agentB", "Message 1");
        Message msg2 = new Message("agentC", "agentB", "Message 2");
        
        // Act
        mailbox.enqueue(msg1);
        mailbox.enqueue(msg2);
        
        // Assert
        assertEquals(2, mailbox.size());
        assertFalse(mailbox.isEmpty());
    }

    @Test
    void testQueueCopy() {
        // Arrange
        Message msg = new Message("agentA", "agentB", "Test");
        mailbox.enqueue(msg);
        
        // Act
        var copy = mailbox.queueCopy();
        
        // Assert
        assertEquals(1, copy.size());
        assertNotSame(mailbox, copy, "Should return a copy, not the same queue");
    }

    @Test
    void testRemove() {
        // Arrange
        Message msg1 = new Message("agentA", "agentB", "Message 1");
        Message msg2 = new Message("agentC", "agentB", "Message 2");
        mailbox.enqueue(msg1);
        mailbox.enqueue(msg2);
        
        // Act
        mailbox.remove(msg1);
        
        // Assert
        assertEquals(1, mailbox.size());
        assertFalse(mailbox.isEmpty());
    }

    @Test
    void testEmptyMailbox() {
        // Assert
        assertTrue(mailbox.isEmpty());
        assertEquals(0, mailbox.size());
    }
}