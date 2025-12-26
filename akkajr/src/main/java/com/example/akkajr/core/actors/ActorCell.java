package com.example.akkajr.core.actors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.example.akkajr.core.observability.MessageEvent;

final class ActorCell {
    private final Actor actor;
    private final ActorPath path;
    private final ActorRef selfRef;
    private final ActorRef parentRef;
    private final ActorSystem system;
    private final Mailbox mailbox;
    private final ExecutorService dispatcher;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final ReentrantLock pauseLock = new ReentrantLock();
    private final Condition unpaused = pauseLock.newCondition();
    private final AtomicLong processed = new AtomicLong(0);
    private final AtomicLong failed = new AtomicLong(0);

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
        system.recordActorStopped();
    }

    void pauseProcessing() {
        pauseLock.lock();
        try {
            paused.set(true);
        } finally {
            pauseLock.unlock();
        }
    }

    void resumeProcessing() {
        pauseLock.lock();
        try {
            if (paused.compareAndSet(true, false)) {
                unpaused.signalAll();
            }
        } finally {
            pauseLock.unlock();
        }
    }

    boolean isPaused() {
        return paused.get();
    }

    int mailboxSize() {
        return mailbox.size();
    }

    long processedCount() {
        return processed.get();
    }

    long failedCount() {
        return failed.get();
    }

    private void awaitIfPaused() throws InterruptedException {
        pauseLock.lock();
        try {
            while (paused.get() && running.get()) {
                unpaused.await();
            }
        } finally {
            pauseLock.unlock();
        }
    }

    private void pump() {
        while (running.get()) {
            try {
                Mailbox.MessageEnvelope envelope = mailbox.take();
                if (mailbox.isPoison(envelope)) {
                    break;
                }
                awaitIfPaused();
                if (!running.get()) {
                    break;
                }
                if (envelope.message instanceof PoisonPill) {
                    system.stop(selfRef);
                    break;
                }
                long start = System.nanoTime();
                actor.logger.info("[ACTOR MSG] path=" + path + " msgId=" + envelope.messageId + " traceId=" + envelope.traceId + " sender=" + (envelope.sender != null ? envelope.sender.path() : "none") + " type=" + envelope.message.getClass().getSimpleName());
                actor.receive(envelope.message, envelope.sender);
                system.recordMessageProcessed(path, System.nanoTime() - start);
                processed.incrementAndGet();
                system.recordEvent(new MessageEvent("processed", System.currentTimeMillis(), path.value(), envelope.messageId, envelope.traceId, null));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                actor.logger.warning("Actor " + path + " failed on message: " + e.getMessage());
                system.recordMessageFailed(path);
                failed.incrementAndGet();
                system.recordEvent(new MessageEvent("failed", System.currentTimeMillis(), path.value(), null, null, e.getMessage()));
            }
        }
    }
}
