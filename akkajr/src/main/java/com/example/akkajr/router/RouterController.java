package com.example.akkajr.controllers;

import com.example.akkajr.messaging.Message;
import com.example.akkajr.messaging.MessageService;
import com.example.akkajr.router.BroadcastRouter;
import com.example.akkajr.router.RoundRobinRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/router")
public class RouterController {

    @Autowired
    private MessageService messageService;

    /**
     * Route un message en Round-Robin vers plusieurs workers
     * POST /api/router/roundrobin
     * Body: {"sender": "client", "workers": ["worker1", "worker2", "worker3"], "content": "Task"}
     */
    @PostMapping("/roundrobin")
    public ResponseEntity<?> routeRoundRobin(@RequestBody RouteRequest request) {
        if (request.workers == null || request.workers.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Workers list is required"));
        }

        RoundRobinRouter router = new RoundRobinRouter(request.workers);
        router.messageService = messageService;

        Message msg = new Message(request.sender, "router", request.content);
        router.route(msg);

        return ResponseEntity.ok(Map.of(
            "message", "Message routed in round-robin",
            "workers", request.workers,
            "content", request.content
        ));
    }

    /**
     * Diffuse un message à tous les workers (Broadcast)
     * POST /api/router/broadcast
     * Body: {"sender": "admin", "workers": ["worker1", "worker2", "worker3"], "content": "Alert!"}
     */
    @PostMapping("/broadcast")
    public ResponseEntity<?> routeBroadcast(@RequestBody RouteRequest request) {
        if (request.workers == null || request.workers.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Workers list is required"));
        }

        BroadcastRouter router = new BroadcastRouter(request.workers);
        router.messageService = messageService;

        Message msg = new Message(request.sender, "router", request.content);
        router.route(msg);

        return ResponseEntity.ok(Map.of(
            "message", "Message broadcasted to all workers",
            "workers", request.workers,
            "content", request.content
        ));
    }

    /**
     * Démo : créer des workers et router des messages
     * POST /api/router/demo
     */
    @PostMapping("/demo")
    public ResponseEntity<?> demoRouting() {
        // Simuler 3 workers
        List<String> workers = List.of("worker1", "worker2", "worker3");

        // Round-robin : envoyer 5 messages
        RoundRobinRouter rrRouter = new RoundRobinRouter(workers);
        rrRouter.messageService = messageService;

        for (int i = 1; i <= 5; i++) {
            Message msg = new Message("demo-client", "router", "Task " + i);
            rrRouter.route(msg);
        }

        // Broadcast : envoyer une alerte à tous
        BroadcastRouter bcRouter = new BroadcastRouter(workers);
        bcRouter.messageService = messageService;

        Message alert = new Message("admin", "router", "SYSTEM ALERT: Maintenance in 5 minutes");
        bcRouter.route(alert);

        return ResponseEntity.ok(Map.of(
            "message", "Demo completed",
            "roundRobinMessages", 5,
            "broadcastMessages", 1,
            "workers", workers
        ));
    }

    // DTO pour les requêtes
    public static class RouteRequest {
        public String sender;
        public List<String> workers;
        public String content;
    }
}
