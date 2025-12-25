package com.example.akkajr.messaging;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgentAddressTest {

    @Test
    void testParseLocalAddress() {
        // Arrange & Act
        AgentAddress address = AgentAddress.parse("agentB");
        
        // Assert
        assertTrue(address.isLocal());
        assertFalse(address.isRemote());
        assertNull(address.serviceName());
        assertEquals("agentB", address.agentId());
        assertEquals("agentB", address.toString());
    }

    @Test
    void testParseRemoteAddress() {
        // Arrange & Act
        AgentAddress address = AgentAddress.parse("service2:agentB");
        
        // Assert
        assertFalse(address.isLocal());
        assertTrue(address.isRemote());
        assertEquals("service2", address.serviceName());
        assertEquals("agentB", address.agentId());
        assertEquals("service2:agentB", address.toString());
    }

    @Test
    void testParseRemoteAddressWithMultipleColons() {
        // Arrange & Act
        AgentAddress address = AgentAddress.parse("service2:agent:B");
        
        // Assert
        assertTrue(address.isRemote());
        assertEquals("service2", address.serviceName());
        assertEquals("agent:B", address.agentId());
    }

    @Test
    void testParseNullAddress() {
        // Assert
        assertThrows(IllegalArgumentException.class, () -> {
            AgentAddress.parse(null);
        }, "Should throw exception for null address");
    }

    @Test
    void testParseBlankAddress() {
        // Assert
        assertThrows(IllegalArgumentException.class, () -> {
            AgentAddress.parse("   ");
        }, "Should throw exception for blank address");
    }

    @Test
    void testParseEmptyAddress() {
        // Assert
        assertThrows(IllegalArgumentException.class, () -> {
            AgentAddress.parse("");
        }, "Should throw exception for empty address");
    }
}