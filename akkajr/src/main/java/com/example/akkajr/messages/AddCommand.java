package com.example.akkajr.messages;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AddCommand implements ServiceCommand {
    private final String serviceId;
    private final String command;
    private final ActorRef<ServiceResponse> replyTo;
    
    @JsonCreator
    public AddCommand(
        @JsonProperty("serviceId") String serviceId,
        @JsonProperty("command") String command,
        @JsonProperty("replyTo") ActorRef<ServiceResponse> replyTo) {
        this.serviceId = serviceId;
        this.command = command;
        this.replyTo = replyTo;
    }
    
    @Override
    public String getServiceId() {
        return serviceId;
    }
    
    public String getCommand() {
        return command;
    }
    
    public ActorRef<ServiceResponse> getReplyTo() {
        return replyTo;
    }
}