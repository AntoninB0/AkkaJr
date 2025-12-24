package com.example.akkajr.core.actors;

/**
 * Root guardian actor. It currently acts as a passive supervisor placeholder.
 * Supervision strategies can be added later (block 3).
 */
public final class GuardianActor extends Actor {
    @Override
    public void receive(Object message, ActorRef sender) {
        // Guardian is passive for now; can be extended for system messages
    }
}
