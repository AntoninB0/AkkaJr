package com.example.akkajr.core.actors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

final class Mailbox {
    private static final MessageEnvelope POISON = new MessageEnvelope(null, null);

    private final BlockingQueue<MessageEnvelope> queue = new LinkedBlockingQueue<>();

    void enqueue(Object message, ActorRef sender) {
        queue.offer(new MessageEnvelope(message, sender));
    }

    MessageEnvelope take() throws InterruptedException {
        return queue.take();
    }

    void close() {
        queue.offer(POISON);
    }

    boolean isPoison(MessageEnvelope envelope) {
        return envelope == POISON;
    }

    static final class MessageEnvelope {
        final Object message;
        final ActorRef sender;

        MessageEnvelope(Object message, ActorRef sender) {
            this.message = message;
            this.sender = sender;
        }
    }
}
