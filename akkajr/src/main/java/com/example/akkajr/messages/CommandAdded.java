package com.example.akkajr.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CommandAdded implements ServiceResponse {
    private final String serviceId;
    private final int totalCommands;
    
    @JsonCreator
    public CommandAdded(
        @JsonProperty("serviceId") String serviceId,
        @JsonProperty("totalCommands") int totalCommands) {
        this.serviceId = serviceId;
        this.totalCommands = totalCommands;
    }
    
    public String getServiceId() {
        return serviceId;
    }
    
    public int getTotalCommands() {
        return totalCommands;
    }
}