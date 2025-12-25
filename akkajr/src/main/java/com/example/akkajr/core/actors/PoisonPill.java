package com.example.akkajr.core.actors;

/**
 * Special message that triggers a graceful stop when processed by the actor.
 */
public final class PoisonPill {
    public static final PoisonPill INSTANCE = new PoisonPill();

    private PoisonPill() {
    }
}
