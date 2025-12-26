package com.example.akkajr.core.metrics;

public final class MetricsSnapshot {
    private final long actorsCreated;
    private final long actorsStopped;
    private final long messagesProcessed;
    private final long messagesFailed;
    private final long totalActors;
    private final long userActors;
    private final long systemActors;
    private final long pausedActors;
    private final long totalBacklog;

    public MetricsSnapshot(long actorsCreated, long actorsStopped, long messagesProcessed, long messagesFailed,
                           long totalActors, long userActors, long systemActors, long pausedActors, long totalBacklog) {
        this.actorsCreated = actorsCreated;
        this.actorsStopped = actorsStopped;
        this.messagesProcessed = messagesProcessed;
        this.messagesFailed = messagesFailed;
        this.totalActors = totalActors;
        this.userActors = userActors;
        this.systemActors = systemActors;
        this.pausedActors = pausedActors;
        this.totalBacklog = totalBacklog;
    }

    public long getActorsCreated() {
        return actorsCreated;
    }

    public long getActorsStopped() {
        return actorsStopped;
    }

    public long getMessagesProcessed() {
        return messagesProcessed;
    }

    public long getMessagesFailed() {
        return messagesFailed;
    }

    public long getTotalActors() {
        return totalActors;
    }

    public long getUserActors() {
        return userActors;
    }

    public long getSystemActors() {
        return systemActors;
    }

    public long getPausedActors() {
        return pausedActors;
    }

    public long getTotalBacklog() {
        return totalBacklog;
    }
}
