package com.example.akkajr.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Heartbeat implements ServiceCommand {
    
    private final String serviceId;
    
    @JsonCreator
    public Heartbeat(@JsonProperty("serviceId") String serviceId) {
        this.serviceId = serviceId;
    }
    
    @Override
    public String getServiceId() {
        return serviceId;
    }
    
    @Override
    public String toString() {
        return "Heartbeat{serviceId='" + serviceId + "'}";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Heartbeat heartbeat = (Heartbeat) o;
        return serviceId.equals(heartbeat.serviceId);
    }
    
    @Override
    public int hashCode() {
        return serviceId.hashCode();
    }
}