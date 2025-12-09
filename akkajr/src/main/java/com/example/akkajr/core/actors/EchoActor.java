package com.example.akkajr.core.actors;

public class EchoActor extends Actor {
    @Override
    public void preStart() {
        logger.info("EchoActor started at " + getContext().getPath());
    }

    @Override
    public void receive(Object message, ActorRef sender) {
        logger.info("EchoActor received: " + message + " from " + (sender != null ? sender.path() : "no-sender"));
    }

    @Override
    public void postStop() {
        logger.info("EchoActor stopped at " + getContext().getPath());
    }
}
