package com.example.akkajr.core.observability;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.akkajr.core.actors.ActorSystem;
import com.example.akkajr.core.metrics.MetricsSnapshot;

@Service
public class ObservabilityService {

    private final ActorSystem actorSystem;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "metrics-sse"));

    private static final long BACKLOG_THRESHOLD = 1000;
    private static final long PAUSED_THRESHOLD = 0;

    public ObservabilityService(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
        startStreaming();
    }

    public SseEmitter register(Duration timeout) {
        SseEmitter emitter = new SseEmitter(timeout.toMillis());
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        return emitter;
    }

    public List<Alert> currentAlerts(MetricsSnapshot snapshot) {
        List<Alert> alerts = new ArrayList<>();
        if (snapshot.getTotalBacklog() > BACKLOG_THRESHOLD) {
            alerts.add(new Alert("WARN", "backlog_threshold", "Backlog total au-dessus de " + BACKLOG_THRESHOLD));
        }
        if (snapshot.getPausedActors() > PAUSED_THRESHOLD) {
            alerts.add(new Alert("INFO", "actors_paused", snapshot.getPausedActors() + " acteur(s) en pause"));
        }
        if (snapshot.getMessagesFailed() > 0) {
            alerts.add(new Alert("WARN", "messages_failed", snapshot.getMessagesFailed() + " messages en erreur"));
        }
        return alerts;
    }

    private void startStreaming() {
        scheduler.scheduleAtFixedRate(() -> {
            MetricsSnapshot snapshot = actorSystem.metricsSnapshot();
            List<Alert> alerts = currentAlerts(snapshot);
            emitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().data(snapshot).name("metrics"));
                    emitter.send(SseEmitter.event().data(alerts).name("alerts"));
                } catch (IOException e) {
                    emitter.complete();
                }
            });
        }, 0, 1, TimeUnit.SECONDS);
    }
}
