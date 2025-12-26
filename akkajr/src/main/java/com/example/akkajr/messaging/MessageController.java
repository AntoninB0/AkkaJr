package com.example.akkajr.messaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    
    @Value("${app.service.name:akkajr}")
    private String currentServiceName;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }
    
    /**
     * Nettoie le receiverId en retirant le préfixe du service actuel
     * et préserve l'origine du message
     */
    private void cleanReceiverId(Message msg) {
        String receiverId = msg.getReceiverId();
        if (receiverId != null && receiverId.startsWith(currentServiceName + ":")) {
            // Préserver l'origine avant de nettoyer
            msg.setOriginService(currentServiceName);
            String cleanedId = receiverId.substring(currentServiceName.length() + 1);
            msg.setReceiverId(cleanedId);
            System.out.println("[CLEAN] receiverId nettoyé: " + receiverId + " -> " + cleanedId + " (origine: " + currentServiceName + ")");
        }
    }

    @PostMapping("/tell")
    public String sendTell(@RequestBody Message msg) {
        cleanReceiverId(msg);
        messageService.send(msg);
        return "TELL envoyé";
    }

    @PostMapping("/ask")
    public ResponseEntity<String> sendAsk(@RequestBody AskMessage ask) {
        cleanReceiverId(ask);
        messageService.send(ask);
        
        // CORRECTION: Attendre la réponse de l'acteur via le CompletableFuture
        // avec un timeout de 30 secondes
        try {
            String response = ask.getFutureResponse().get(30, TimeUnit.SECONDS);
            return ResponseEntity.ok(response);
        } catch (java.util.concurrent.TimeoutException e) {
            System.err.println("[ASK TIMEOUT] Timeout en attendant la réponse pour " + ask.getReceiverId());
            return ResponseEntity.status(504).body("Timeout: Aucune réponse reçue dans les 30 secondes");
        } catch (Exception e) {
            System.err.println("[ASK ERROR] Erreur en attendant la réponse: " + e.getMessage());
            return ResponseEntity.status(500).body("Erreur: " + e.getMessage());
        }
    }

    @GetMapping("/inbox/{id}")
    public Queue<Message> inbox(@PathVariable String id) {
        return messageService.inbox(id);
    }

    @GetMapping("/history")
    public Queue<Message> history() {
        return messageService.history();
    }

    @GetMapping("/deadletters")
    public Queue<Message> deadLetters() {
        return messageService.getDeadLetters();
    }

    @PostMapping("/reply")
    public String replyToAsk(@RequestParam String agentId, @RequestBody String response) {
        messageService.replyToAsk(agentId, response);
        return "Réponse envoyée à l'ASK";
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Queue<Message> history = messageService.history();
        Queue<Message> deadLetters = messageService.getDeadLetters();
        
        // Compter les messages remote en utilisant originService au lieu de receiverId
        long remoteCount = history.stream()
            .filter(m -> m.getOriginService() != null && !m.getOriginService().isEmpty())
            .count();
        
        long pendingAsks = history.stream()
            .filter(m -> m instanceof AskMessage)
            .map(m -> (AskMessage) m)
            .filter(a -> !a.getFutureResponse().isDone())
            .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMessages", history.size());
        stats.put("remoteMessages", remoteCount);
        stats.put("deadLetters", deadLetters.size());
        stats.put("pendingAsks", pendingAsks);
        
        return ResponseEntity.ok(stats);
    }


    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getLogs() {
        Queue<Message> history = messageService.history();
        Queue<Message> deadLetters = messageService.getDeadLetters();
        
        List<Map<String, Object>> logEntries = new ArrayList<>();
        
        // Convert recent messages to log entries (last 50)
        history.stream()
            .limit(50)
            .forEach(msg -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("timestamp", msg.getTimestamp());
                entry.put("type", msg instanceof AskMessage ? "ASK" : "TELL");
                entry.put("sender", msg.getSenderId());
                entry.put("receiver", msg.getReceiverId());
                entry.put("content", msg.getContent());
                entry.put("origin", msg.getOriginService());
                entry.put("isRemote", msg.getOriginService() != null && !msg.getOriginService().isEmpty());
                logEntries.add(entry);
            });
        
        // Add dead letters as error logs
        deadLetters.stream()
            .limit(20)
            .forEach(msg -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("timestamp", msg.getTimestamp());
                entry.put("type", "DEAD_LETTER");
                entry.put("sender", msg.getSenderId());
                entry.put("receiver", msg.getReceiverId());
                entry.put("content", msg.getContent());
                entry.put("origin", msg.getOriginService());
                entry.put("isRemote", false);
                logEntries.add(entry);
            });
        
        // Sort by timestamp (newest first)
        logEntries.sort((a, b) -> Long.compare(
            (Long) b.getOrDefault("timestamp", 0L),
            (Long) a.getOrDefault("timestamp", 0L)
        ));
        
        Map<String, Object> logs = new HashMap<>();
        logs.put("entries", logEntries);
        logs.put("serviceName", currentServiceName);
        logs.put("totalLogs", logEntries.size());
        
        return ResponseEntity.ok(logs);
    }}