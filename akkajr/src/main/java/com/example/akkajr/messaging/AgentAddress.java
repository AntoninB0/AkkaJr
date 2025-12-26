package com.example.akkajr.messaging;

public record AgentAddress(String serviceName, String agentId) {
    
    /**
     * Parse une adresse d'agent au format "serviceName:agentId" ou juste "agentId"
     */
    public static AgentAddress parse(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address cannot be null or blank");
        }
        
        int colonIndex = address.indexOf(':');
        if (colonIndex == -1) {
            // Pas de ':', c'est un agent local
            return new AgentAddress(null, address);
        }
        
        String serviceName = address.substring(0, colonIndex);
        String agentId = address.substring(colonIndex + 1);
        
        return new AgentAddress(serviceName, agentId);
    }
    
    public boolean isLocal() {
        return serviceName == null;
    }
    
    public boolean isRemote() {
        return serviceName != null;
    }
    
    @Override
    public String toString() {
        return isLocal() ? agentId : serviceName + ":" + agentId;
    }
}
