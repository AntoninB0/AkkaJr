package com.example.akkajr.messages;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StartService implements ServiceCommand {
    private final String serviceId;
    private final ActorRef<ServiceResponse> replyTo;
    
    @JsonCreator
    public StartService(
        @JsonProperty("serviceId") String serviceId,
        @JsonProperty("replyTo") ActorRef<ServiceResponse> replyTo) {
        this.serviceId = serviceId;
        this.replyTo = replyTo;
    }
    
    @Override
    public String getServiceId() {
        return serviceId;
    }
    
    public ActorRef<ServiceResponse> getReplyTo() {
        return replyTo;
    }
}