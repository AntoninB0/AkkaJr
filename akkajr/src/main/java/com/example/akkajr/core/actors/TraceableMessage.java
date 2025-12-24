package com.example.akkajr.core.actors;

/**
 * Optional marker for messages that carry a trace identifier.
 */
public interface TraceableMessage {
    String traceId();
}
