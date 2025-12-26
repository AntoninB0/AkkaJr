package com.example.akkajr.core.metrics;

public final class ActorState {
    private final String path;
    private final long backlog;
    private final boolean paused;
    private final boolean guardian;
    private final String scope;
    private final long processed;
    private final long failed;

    public ActorState(String path, long backlog, boolean paused, boolean guardian, String scope, long processed, long failed) {
        this.path = path;
        this.backlog = backlog;
        this.paused = paused;
        this.guardian = guardian;
        this.scope = scope;
        this.processed = processed;
        this.failed = failed;
    }

    public String getPath() {
        return path;
    }

    public long getBacklog() {
        return backlog;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isGuardian() {
        return guardian;
    }

    public String getScope() {
        return scope;
    }

    public long getProcessed() {
        return processed;
    }

    public long getFailed() {
        return failed;
    }
}