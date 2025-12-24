package com.example.akkajr.core.actors;

import java.util.Objects;

public final class ActorPath {
    public static final String ROOT_USER = "/user";
    public static final String ROOT_SYSTEM = "/system";

    private final String path;

    public ActorPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("ActorPath cannot be blank");
        }
        if (!path.startsWith(ROOT_USER) && !path.startsWith(ROOT_SYSTEM)) {
            throw new IllegalArgumentException("ActorPath must start with " + ROOT_USER + " or " + ROOT_SYSTEM);
        }
        this.path = path;
    }

    public ActorPath child(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Child name cannot be blank");
        }
        return new ActorPath(path + "/" + name);
    }

    /**
     * Returns the parent path or null if this is the root (/user).
     */
    public ActorPath parent() {
        String rootPrefix = rootPrefix();
        if (rootPrefix.equals(path)) {
            return null;
        }
        int idx = path.lastIndexOf('/');
        if (idx <= rootPrefix.length()) {
            return new ActorPath(rootPrefix);
        }
        return new ActorPath(path.substring(0, idx));
    }

    public String name() {
        int idx = path.lastIndexOf('/');
        return (idx >= 0 && idx + 1 < path.length()) ? path.substring(idx + 1) : path;
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

    private String rootPrefix() {
        return path.startsWith(ROOT_SYSTEM) ? ROOT_SYSTEM : ROOT_USER;
    }
}
