package com.example.akkajr.core.actors;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ActorSystem {
    private final Map<String, ActorCell> cells = new ConcurrentHashMap<>();
    private final ActorPath root = new ActorPath(ActorPath.ROOT);
    private final AtomicInteger counter = new AtomicInteger(0);

    public ActorRef actorOf(Props props, String name) {
        return actorOfInternal(props, name, null);
    }

    public ActorRef actorOf(Props props) {
        return actorOf(props, null);
    }

    /**
     * Create a child actor under the given parent path, e.g. /user/parent/child.
     */
    public ActorRef actorOfChild(Props props, String name, ActorRef parent) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent ref cannot be null for child actor creation");
        }
        return actorOfInternal(props, name, parent);
    }

    private ActorRef actorOfInternal(Props props, String name, ActorRef parentRef) {
        if (props == null) {
            throw new IllegalArgumentException("Props cannot be null");
        }
        String actorName = (name == null || name.isBlank()) ? "actor-" + counter.incrementAndGet() : name;
        ActorPath basePath = (parentRef == null) ? root : parentRef.path();
        if (parentRef != null && !cells.containsKey(parentRef.path().value())) {
            throw new IllegalArgumentException("Parent actor does not exist: " + parentRef.path());
        }
        ActorPath path = basePath.child(actorName);
        if (cells.containsKey(path.value())) {
            throw new IllegalArgumentException("Actor with path " + path + " already exists");
        }
        Actor actor = props.instantiate();
        ActorCell cell = new ActorCell(actor, path, parentRef, this);
        cells.put(path.value(), cell);
        cell.start();
        return cell.ref();
    }

    public void stop(ActorRef ref) {
        if (ref == null) {
            return;
        }
        ActorCell cell = cells.remove(ref.path().value());
        if (cell != null) {
            cell.stop();
        }
    }

    public void shutdown() {
        cells.values().forEach(ActorCell::stop);
        cells.clear();
    }
}
