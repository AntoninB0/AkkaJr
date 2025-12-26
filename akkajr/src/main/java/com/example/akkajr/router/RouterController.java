package com.example.akkajr.router;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.akkajr.messaging.Message;

/**
 * Controller pour les routers STATIQUES (anciens)
 * 
 * Les nouveaux routers dynamiques sont dans DynamicRouterController
 * 
 * Endpoints :
 * - /api/router/roundrobin (ancien - statique) - OPTIONNEL
 * - /api/router/broadcast (ancien - statique) - OPTIONNEL
 */
@RestController
@RequestMapping("/api/router")
public class RouterController {
    
    private final RoundRobinRouter roundRobinRouter;
    private final BroadcastRouter broadcastRouter;
    
    public RouterController(
            @Autowired(required = false) RoundRobinRouter roundRobinRouter,
            @Autowired(required = false) BroadcastRouter broadcastRouter) {
        this.roundRobinRouter = roundRobinRouter;
        this.broadcastRouter = broadcastRouter;
    }
    
    /**
     * Route Round-Robin STATIQUE (ancien comportement)
     * POST /api/router/roundrobin
     * 
     * IMPORTANT: Cet endpoint utilise l'ancien router avec liste manuelle de workers.
     * Pour le routage dynamique, utilisez /api/router/dynamic/roundrobin
     * 
     * Body: {
     *   "sender": "client",
     *   "recipients": ["worker1", "worker2", "worker3"],
     *   "content": "Message content"
     * }
     */
    @PostMapping("/roundrobin")
    public ResponseEntity<?> routeRoundRobin(@RequestBody RouteRequest request) {
        if (roundRobinRouter == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "RoundRobinRouter non disponible");
            error.put("message", "Utilisez /api/router/dynamic/roundrobin pour le routage dynamique");
            return ResponseEntity.status(503).body(error);
        }
        
        // Creer un Message pour chaque recipient
        for (String recipient : request.getRecipients()) {
            Message message = new Message(
                request.getSender(),
                recipient,
                request.getContent()
            );
            roundRobinRouter.route(message);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "Messages routed via static round-robin");
        response.put("recipientCount", request.getRecipients().size());
        response.put("strategy", "STATIC_ROUND_ROBIN");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Route Broadcast STATIQUE (ancien comportement)
     * POST /api/router/broadcast
     * 
     * Body: {
     *   "sender": "client",
     *   "recipients": ["worker1", "worker2", "worker3"],
     *   "content": "Message content"
     * }
     */
    @PostMapping("/broadcast")
    public ResponseEntity<?> routeBroadcast(@RequestBody RouteRequest request) {
        if (broadcastRouter == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "BroadcastRouter non disponible");
            error.put("message", "Ce endpoint necessite BroadcastRouter");
            return ResponseEntity.status(503).body(error);
        }
        
        // Creer un Message pour chaque recipient
        for (String recipient : request.getRecipients()) {
            Message message = new Message(
                request.getSender(),
                recipient,
                request.getContent()
            );
            broadcastRouter.route(message);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "Messages broadcasted to all recipients");
        response.put("recipientCount", request.getRecipients().size());
        response.put("strategy", "BROADCAST");
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== DTOs ====================
    
    /**
     * Requete pour routage statique
     */
    public static class RouteRequest {
        private String sender;
        private List<String> recipients;
        private String content;
        
        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
        
        public List<String> getRecipients() { return recipients; }
        public void setRecipients(List<String> recipients) { this.recipients = recipients; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}