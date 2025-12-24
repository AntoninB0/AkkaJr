package com.example.akkajr.core.actors;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.akkajr.core.metrics.MetricsRegistry;
import com.example.akkajr.core.metrics.MetricsSnapshot;
import com.example.akkajr.core.metrics.ActorState;
import com.example.akkajr.core.observability.MessageEvent;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;

public final class ActorSystem {
    private final Map<String, ActorCell> cells = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> children = new ConcurrentHashMap<>();
    private final ActorPath userRoot = new ActorPath(ActorPath.ROOT_USER);
    private final ActorPath systemRoot = new ActorPath(ActorPath.ROOT_SYSTEM);
    private final AtomicInteger counter = new AtomicInteger(0);
    private final MetricsRegistry metrics = new MetricsRegistry();
    private final ActorRef userGuardian;
    private final ActorRef systemGuardian;
    private final MeterRegistry meterRegistry;
    private final Deque<MessageEvent> recentEvents = new ArrayDeque<>(256);
    private final ReentrantLock eventsLock = new ReentrantLock();

    public ActorSystem() {
        this(null);
    }

    public ActorSystem(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.userGuardian = createGuardian(userRoot);
        this.systemGuardian = createGuardian(systemRoot);
    }

    public ActorRef actorOf(Props props, String name) {
        return actorOfInternal(props, name, null, null);
    }

    public ActorRef actorOf(Props props) {
        return actorOf(props, null);
    }

    /**
     * Create an actor under the /system guardian (for internal services).
     */
    public ActorRef actorOfSystem(Props props, String name) {
        return actorOfInternal(props, name, null, systemRoot);
    }

    /**
     * Create a child actor under the given parent path, e.g. /user/parent/child.
     */
    public ActorRef actorOfChild(Props props, String name, ActorRef parent) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent ref cannot be null for child actor creation");
        }
        return actorOfInternal(props, name, parent, null);
    }

    private ActorRef actorOfInternal(Props props, String name, ActorRef parentRef, ActorPath baseOverride) {
        if (props == null) {
            throw new IllegalArgumentException("Props cannot be null");
        }
        String actorName = (name == null || name.isBlank()) ? "actor-" + counter.incrementAndGet() : name;
        ActorPath basePath = (baseOverride != null) ? baseOverride : (parentRef == null ? userRoot : parentRef.path());
        if (parentRef != null && !cells.containsKey(parentRef.path().value())) {
            throw new IllegalArgumentException("Parent actor does not exist: " + parentRef.path());
        }
        ActorPath path = basePath.child(actorName);
        Set<String> siblings = children.computeIfAbsent(basePath.value(), k -> ConcurrentHashMap.newKeySet());
        if (siblings.contains(path.value())) {
            throw new IllegalArgumentException("Actor with name " + actorName + " already exists under " + basePath);
        }
        Actor actor = props.instantiate();
        ActorCell cell = new ActorCell(actor, path, parentRef, this);
        cells.put(path.value(), cell);
        siblings.add(path.value());
        cell.start();
        metrics.recordActorCreated();
        if (meterRegistry != null) {
            meterRegistry.counter("actor.created").increment();
        }
        return cell.ref();
    }

    public void stop(ActorRef ref) {
        if (ref == null) {
            return;
        }
        stopRecursive(ref.path(), false);
    }

    public void pause(ActorRef ref) {
        if (ref == null) {
            return;
        }
        ActorCell cell = cells.get(ref.path().value());
        if (cell != null) {
            cell.pauseProcessing();
        }
    }

    public void resume(ActorRef ref) {
        if (ref == null) {
            return;
        }
        ActorCell cell = cells.get(ref.path().value());
        if (cell != null) {
            cell.resumeProcessing();
        }
    }

    public void sendPoisonPill(ActorRef ref) {
        if (ref != null) {
            ref.tell(PoisonPill.INSTANCE, null);
        }
    }

    public void recordMessageProcessed(ActorPath path, long nanos) {
        metrics.recordMessageProcessed();
        if (meterRegistry != null) {
            meterRegistry.counter("actor.messages.processed", "actor", path.value()).increment();
            Timer timer = meterRegistry.timer("actor.messages.latency", "actor", path.value());
            timer.record(nanos, TimeUnit.NANOSECONDS);
        }
    }

    public void recordMessageFailed(ActorPath path) {
        metrics.recordMessageFailed();
        if (meterRegistry != null) {
            meterRegistry.counter("actor.messages.failed", "actor", path.value()).increment();
        }
    }

    void recordActorStopped() {
        metrics.recordActorStopped();
        if (meterRegistry != null) {
            meterRegistry.counter("actor.stopped").increment();
        }
    }

    private void stopRecursive(ActorPath path, boolean allowGuardian) {
        if (isGuardian(path) && !allowGuardian) {
            return; // guardians are managed by the system
        }
        Set<String> childPaths = children.getOrDefault(path.value(), Collections.emptySet());
        for (String childPath : childPaths.toArray(new String[0])) {
            stopRecursive(new ActorPath(childPath), allowGuardian);
        }
        children.remove(path.value());
        ActorPath parent = path.parent();
        if (parent != null) {
            children.computeIfPresent(parent.value(), (k, set) -> {
                set.remove(path.value());
                return set.isEmpty() ? null : set;
            });
        }
        ActorCell cell = cells.remove(path.value());
        if (cell != null) {
            cell.stop();
        }
    }

    public void shutdown() {
        // stop from the user and system roots downwards
        Set<String> roots = children.getOrDefault(userRoot.value(), Collections.emptySet());
        for (String child : roots.toArray(new String[0])) {
            stopRecursive(new ActorPath(child), true);
        }
        Set<String> systemChildren = children.getOrDefault(systemRoot.value(), Collections.emptySet());
        for (String child : systemChildren.toArray(new String[0])) {
            stopRecursive(new ActorPath(child), true);
        }
        // finally stop guardians themselves
        stopRecursive(userRoot, true);
        stopRecursive(systemRoot, true);
        cells.clear();
        children.clear();
    }

    /**
     * Look up an actor by absolute path (e.g. /user/foo/bar). Returns null if not found.
     */
    public ActorRef actorSelection(String absolutePath) {
        ActorCell cell = cells.get(absolutePath);
        return cell != null ? cell.ref() : null;
    }

    public MetricsSnapshot metricsSnapshot() {
        long totalActors = cells.size();
        long userActors = children.getOrDefault(userRoot.value(), Collections.emptySet()).size();
        long systemActors = children.getOrDefault(systemRoot.value(), Collections.emptySet()).size();
        long pausedActors = cells.values().stream().filter(ActorCell::isPaused).count();
        long backlog = cells.values().stream().mapToLong(ActorCell::mailboxSize).sum();
        return metrics.snapshot(totalActors, userActors, systemActors, pausedActors, backlog);
    }

    public List<ActorState> actorStates() {
        List<ActorState> states = new ArrayList<>();
        for (Map.Entry<String, ActorCell> entry : cells.entrySet()) {
            ActorCell cell = entry.getValue();
            ActorPath path = cell.path();
            boolean guardian = isGuardian(path);
            String scope = path.value().startsWith(ActorPath.ROOT_USER) ? "user" : "system";
            states.add(new ActorState(path.value(), cell.mailboxSize(), cell.isPaused(), guardian, scope, cell.processedCount(), cell.failedCount()));
        }
        return states;
    }

    void recordEvent(MessageEvent event) {
        eventsLock.lock();
        try {
            if (recentEvents.size() >= 200) {
                recentEvents.removeFirst();
            }
            recentEvents.addLast(event);
        } finally {
            eventsLock.unlock();
        }
    }

    public List<MessageEvent> recentEvents() {
        eventsLock.lock();
        try {
            return new ArrayList<>(recentEvents);
        } finally {
            eventsLock.unlock();
        }
    }

    private ActorRef createGuardian(ActorPath rootPath) {
        Actor actor = new GuardianActor();
        ActorCell cell = new ActorCell(actor, rootPath, null, this);
        cells.put(rootPath.value(), cell);
        cell.start();
        return cell.ref();
    }

    private boolean isGuardian(ActorPath path) {
        String value = path.value();
        return userRoot.value().equals(value) || systemRoot.value().equals(value);
    }
}
