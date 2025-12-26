package com.example.akkajr.core.metrics;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.example.akkajr.core.actors.ActorSystem;

@Component
public class ActorSystemHealthIndicator implements HealthIndicator {

    private final ActorSystem actorSystem;
    private static final long BACKLOG_THRESHOLD = 1000;

    public ActorSystemHealthIndicator(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    @Override
    public Health health() {
        MetricsSnapshot snapshot = actorSystem.metricsSnapshot();
        Health.Builder builder = Health.up()
                .withDetail("actors.total", snapshot.getTotalActors())
                .withDetail("actors.paused", snapshot.getPausedActors())
                .withDetail("backlog.total", snapshot.getTotalBacklog());

        if (snapshot.getTotalBacklog() > BACKLOG_THRESHOLD) {
            builder = builder.status("OUT_OF_SERVICE").withDetail("reason", "backlog_threshold_exceeded");
        }
        if (snapshot.getPausedActors() > 0) {
            builder = builder.withDetail("notice", "some actors are paused");
        }
        return builder.build();
    }
}
