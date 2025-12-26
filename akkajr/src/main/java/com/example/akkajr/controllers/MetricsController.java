package com.example.akkajr.controllers;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.akkajr.core.actors.ActorSystem;
import com.example.akkajr.core.metrics.ActorState;
import com.example.akkajr.core.metrics.MetricsSnapshot;
import com.example.akkajr.core.observability.Alert;
import com.example.akkajr.core.observability.MessageEvent;
import com.example.akkajr.core.observability.ObservabilityService;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("coreActorSystem")
    private ActorSystem actorSystem;

    @Autowired
    private ObservabilityService observabilityService;

    @GetMapping("/actors")
    public ResponseEntity<MetricsSnapshot> actorsMetrics() {
        MetricsSnapshot snapshot = actorSystem.metricsSnapshot();
        return ResponseEntity.ok(snapshot);
    }

    @GetMapping("/actors/detail")
    public ResponseEntity<List<ActorState>> actorsDetail() {
        return ResponseEntity.ok(actorSystem.actorStates());
    }

    @GetMapping("/events")
    public ResponseEntity<List<MessageEvent>> events() {
        return ResponseEntity.ok(actorSystem.recentEvents());
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<Alert>> alerts() {
        MetricsSnapshot snapshot = actorSystem.metricsSnapshot();
        return ResponseEntity.ok(observabilityService.currentAlerts(snapshot));
    }

    @GetMapping("/stream")
    public SseEmitter stream() {
        return observabilityService.register(Duration.ofSeconds(30));
    }
}
