package com.example.akkajr.messaging;

import java.util.concurrent.CompletableFuture;

public class AskMessage extends Message {

    private final CompletableFuture<String> futureResponse = new CompletableFuture<>();

    public AskMessage(String senderId, String receiverId, String content) {
        super(senderId, receiverId, content);
    }

    public CompletableFuture<String> getFutureResponse() {
        return futureResponse;
    }

    public void complete(String response) {
        futureResponse.complete(response);
    }
}
