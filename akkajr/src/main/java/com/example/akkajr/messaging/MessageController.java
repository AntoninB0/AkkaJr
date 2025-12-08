package com.example.akkajr.messaging;

import org.springframework.web.bind.annotation.*;

import java.util.Queue;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/tell")
    public String sendTell(@RequestBody Message msg) {
        messageService.send(msg);
        return "TELL envoyé";
    }

    @PostMapping("/ask")
    public String sendAsk(@RequestBody AskMessage ask) {
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
}
