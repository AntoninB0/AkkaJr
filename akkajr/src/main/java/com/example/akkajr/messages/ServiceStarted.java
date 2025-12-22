package com.example.akkajr.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceStarted implements ServiceResponse {
    private final String serviceId;
    
    @JsonCreator
    public ServiceStarted(@JsonProperty("serviceId") String serviceId) {
        this.serviceId = serviceId;
    }
    
    public String getServiceId() {
        return serviceId;
    }
}