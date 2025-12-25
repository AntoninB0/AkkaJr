package com.example.akkajr.core.metrics;

import java.util.concurrent.atomic.AtomicLong;

public final class MetricsRegistry {
    private final AtomicLong actorsCreated = new AtomicLong();
    private final AtomicLong actorsStopped = new AtomicLong();
    private final AtomicLong messagesProcessed = new AtomicLong();
    private final AtomicLong messagesFailed = new AtomicLong();

    public void recordActorCreated() {
        actorsCreated.incrementAndGet();
    }

    public void recordActorStopped() {
        actorsStopped.incrementAndGet();
    }

    public void recordMessageProcessed() {
        messagesProcessed.incrementAndGet();
    }

    public void recordMessageFailed() {
        messagesFailed.incrementAndGet();
    }

    public MetricsSnapshot snapshot(long totalActors, long userActors, long systemActors, long pausedActors, long totalBacklog) {
        return new MetricsSnapshot(
                actorsCreated.get(),
                actorsStopped.get(),
                messagesProcessed.get(),
                messagesFailed.get(),
                totalActors,
                userActors,
                systemActors,
                pausedActors,
                totalBacklog
        );
    }
}
