package com.example.akkajr.messaging;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CompletableFuture;

@Service
public class RemoteMessageClient {
    
    private final WebClient webClient;
    
    public RemoteMessageClient() {
        this.webClient = WebClient.builder()
            .build();
    }
    
    /**
     * Envoie un TELL vers un autre microservice
     */
    public void sendTell(@NonNull Message msg, @NonNull String baseUrl) {
        System.out.println("[REMOTE TELL] Message envoyé à " + baseUrl);
        webClient.post()
                .uri(baseUrl + "/api/messages/tell")
                .bodyValue(msg)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                    result -> System.out.println("[REMOTE TELL] Réponse: " + result),
                    error -> System.err.println("[REMOTE TELL] Erreur: " + error.getMessage())
                );
    }
    
    /**
     * Envoie un ASK vers un autre microservice et retourne la réponse
     */
    public CompletableFuture<String> sendAsk(@NonNull AskMessage msg, @NonNull String baseUrl) {
        System.out.println("[REMOTE ASK] ASK envoyé à " + baseUrl);
        // Note: originService sera défini par le MessageController du service distant
        return webClient.post()
                .uri(baseUrl + "/api/messages/ask")
                .bodyValue(msg)
                .retrieve()
                .bodyToMono(String.class)
                .toFuture();
    }
}
