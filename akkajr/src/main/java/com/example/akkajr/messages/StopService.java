package com.example.akkajr.messages;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StopService implements ServiceCommand {
    private final String serviceId;
    private final ActorRef<ServiceResponse> replyTo;
    
    @JsonCreator
    public StopService(
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