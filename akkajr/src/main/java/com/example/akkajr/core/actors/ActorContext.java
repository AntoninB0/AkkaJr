package com.example.akkajr.core.actors;

public final class ActorContext {
    private final ActorSystem system;
    private final ActorRef self;
    private final ActorRef parent;
    private final ActorPath path;

    public ActorContext(ActorSystem system, ActorRef self, ActorRef parent, ActorPath path) {
        this.system = system;
        this.self = self;
        this.parent = parent;
        this.path = path;
    }

    public ActorSystem getSystem() {
        return system;
    }

    public ActorRef getSelf() {
        return self;
    }

    public ActorRef getParent() {
        return parent;
    }

    public ActorPath getPath() {
        return path;
    }

    /**
     * Convenience to create a child actor under this actor's path.
     */
    public ActorRef actorOf(Props props, String name) {
        return system.actorOfChild(props, name, self);
    }

    public ActorRef actorOf(Props props) {
        return actorOf(props, null);
    }

    /**
     * Create an actor under the /system guardian (for internal services).
     */
    public ActorRef actorOfSystem(Props props, String name) {
        return system.actorOfSystem(props, name);
    }

    public void stop(ActorRef ref) {
        system.stop(ref);
    }

    public void stopSelf() {
        system.stop(self);
    }

    public void pause(ActorRef ref) {
        system.pause(ref);
    }

    public void pauseSelf() {
        system.pause(self);
    }

    public void resume(ActorRef ref) {
        system.resume(ref);
    }

    public void resumeSelf() {
        system.resume(self);
    }

    /**
     * Look up an actor by absolute path (e.g. /user/parent/child).
     */
    public ActorRef actorSelection(String absolutePath) {
        return system.actorSelection(absolutePath);
    }
}
