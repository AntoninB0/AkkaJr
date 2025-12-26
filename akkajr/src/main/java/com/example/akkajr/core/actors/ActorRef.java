package com.example.akkajr.core.actors;

import java.util.Objects;

public final class ActorRef {
    private final ActorCell cell;
    private final ActorPath path;

    ActorRef(ActorCell cell, ActorPath path) {
        this.cell = cell;
        this.path = path;
    }

    public void tell(Object message, ActorRef sender) {
        if (cell == null) {
            throw new IllegalStateException("ActorRef is not attached to a cell");
        }
        cell.enqueue(message, sender);
    }

    public ActorPath path() {
        return path;
    }

    @Override
    public String toString() {
        return path.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActorRef)) return false;
        ActorRef actorRef = (ActorRef) o;
        return Objects.equals(path, actorRef.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
