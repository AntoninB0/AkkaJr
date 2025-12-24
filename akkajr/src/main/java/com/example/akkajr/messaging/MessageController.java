package com.example.akkajr.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

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
    public String sendAsk(@RequestBody AskMessage ask) {
        cleanReceiverId(ask);
        messageService.send(ask);
        return "ASK envoyé, attente de réponse";
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
}