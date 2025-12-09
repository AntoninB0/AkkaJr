package com.example.akkajr.core.actors;

import java.util.logging.Logger;

public abstract class Actor {
    private ActorContext context;
    protected final Logger logger = Logger.getLogger(getClass().getName());

    void setContext(ActorContext context) {
        this.context = context;
    }

    public ActorContext getContext() {
        return context;
    }

    // Lifecycle hooks
    public void preStart() throws Exception {
        // optional override
    }

    public abstract void receive(Object message, ActorRef sender) throws Exception;

    public void postStop() throws Exception {
        // optional override
    }
}
