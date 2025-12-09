package com.example.akkajr.core.actors;

import java.util.Objects;

public final class ActorPath {
    public static final String ROOT = "/user";

    private final String path;

    public ActorPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("ActorPath cannot be blank");
        }
        if (!path.startsWith(ROOT)) {
            throw new IllegalArgumentException("ActorPath must start with " + ROOT);
        }
        this.path = path;
    }

    public ActorPath child(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Child name cannot be blank");
        }
        return new ActorPath(path + "/" + name);
    }

    public String value() {
        return path;
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActorPath)) return false;
        ActorPath actorPath = (ActorPath) o;
        return Objects.equals(path, actorPath.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
