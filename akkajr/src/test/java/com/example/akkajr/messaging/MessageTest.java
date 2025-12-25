package com.example.akkajr.messaging;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void testMessageCreation() {
        // Arrange & Act
        Message msg = new Message("sender1", "receiver1", "Hello");
        
        // Assert
        assertEquals("sender1", msg.getSenderId());
        assertEquals("receiver1", msg.getReceiverId());
        assertEquals("Hello", msg.getContent());
        assertTrue(msg.getTimestamp() > 0);
        assertNull(msg.getOriginService());
    }

    @Test
    void testMessageSetters() {
        // Arrange
        Message msg = new Message();
        
        // Act
        msg.setSenderId("sender1");
        msg.setReceiverId("receiver1");
        msg.setContent("Hello");
        msg.setTimestamp(12345L);
        msg.setOriginService("service1");
        
        // Assert
        assertEquals("sender1", msg.getSenderId());
        assertEquals("receiver1", msg.getReceiverId());
        assertEquals("Hello", msg.getContent());
        assertEquals(12345L, msg.getTimestamp());
        assertEquals("service1", msg.getOriginService());
    }

    @Test
    void testMessageToString() {
        // Arrange
        Message msg = new Message("sender1", "receiver1", "Hello");
        msg.setOriginService("service1");
        
        // Act
        String str = msg.toString();
        
        // Assert
        assertTrue(str.contains("sender1"));
        assertTrue(str.contains("receiver1"));
        assertTrue(str.contains("Hello"));
        assertTrue(str.contains("service1"));
    }
}