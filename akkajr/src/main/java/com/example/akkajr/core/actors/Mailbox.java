package com.example.akkajr.core.actors;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

final class Mailbox {
    private static final MessageEnvelope POISON = new MessageEnvelope(null, null, "poison", "poison");

    private final BlockingQueue<MessageEnvelope> queue = new LinkedBlockingQueue<>();

    void enqueue(Object message, ActorRef sender) {
        String traceId = (message instanceof TraceableMessage) ? ((TraceableMessage) message).traceId() : UUID.randomUUID().toString();
        String messageId = UUID.randomUUID().toString();
        queue.offer(new MessageEnvelope(message, sender, messageId, traceId));
    }

    MessageEnvelope take() throws InterruptedException {
        return queue.take();
    }

    int size() {
        return queue.size();
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
        final String messageId;
        final String traceId;

        MessageEnvelope(Object message, ActorRef sender, String messageId, String traceId) {
            this.message = message;
            this.sender = sender;
            this.messageId = messageId;
            this.traceId = traceId;
        }
    }
}
