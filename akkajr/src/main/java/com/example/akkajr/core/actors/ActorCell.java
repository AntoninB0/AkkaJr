package com.example.akkajr.core.actors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

final class ActorCell {
    private final Actor actor;
    private final ActorPath path;
    private final ActorRef selfRef;
    private final ActorRef parentRef;
    private final ActorSystem system;
    private final Mailbox mailbox;
    private final ExecutorService dispatcher;
    private final AtomicBoolean running = new AtomicBoolean(false);

    ActorCell(Actor actor, ActorPath path, ActorRef parentRef, ActorSystem system) {
        this.actor = actor;
        this.path = path;
        this.parentRef = parentRef;
        this.system = system;
        this.mailbox = new Mailbox();
        this.selfRef = new ActorRef(this, path);
        this.dispatcher = Executors.newSingleThreadExecutor(r -> new Thread(r, "actor-" + path.value()));
    }

    ActorRef ref() {
        return selfRef;
    }

    ActorPath path() {
        return path;
    }

    void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        actor.setContext(new ActorContext(system, selfRef, parentRef, path));
        try {
            actor.preStart();
        } catch (Exception e) {
            throw new IllegalStateException("Actor preStart failed for " + path, e);
        }
        dispatcher.submit(this::pump);
    }

    void enqueue(Object message, ActorRef sender) {
        mailbox.enqueue(message, sender);
    }

    void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        mailbox.close();
        dispatcher.shutdownNow();
        try {
            actor.postStop();
        } catch (Exception e) {
            // keep shutdown resilient
            actor.logger.warning("postStop failed for " + path + ": " + e.getMessage());
        }
    }

    private void pump() {
        while (running.get()) {
            try {
                Mailbox.MessageEnvelope envelope = mailbox.take();
                if (mailbox.isPoison(envelope)) {
                    break;
                }
                actor.receive(envelope.message, envelope.sender);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                actor.logger.warning("Actor " + path + " failed on message: " + e.getMessage());
            }
        }
    }
}
