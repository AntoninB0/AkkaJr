package com.example.akkajr.messaging;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class RemoteMessageClient {
    
    private final WebClient webClient;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    
    public RemoteMessageClient() {
        this.webClient = WebClient.builder()
            .build();
    }
    
    /**
     * Envoie un TELL vers un autre microservice
     * CORRECTION: Amélioration de la gestion d'erreur avec timeout
     */
    public void sendTell(@NonNull Message msg, @NonNull String baseUrl) {
        System.out.println("[REMOTE TELL] Message envoyé à " + baseUrl);
        webClient.post()
                .uri(baseUrl + "/api/messages/tell")
                .bodyValue(msg)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(TIMEOUT)
                .subscribe(
                    result -> System.out.println("[REMOTE TELL] Réponse: " + result),
                    error -> {
                        System.err.println("[REMOTE TELL ERROR] Erreur lors de l'envoi vers " + baseUrl + ": " + error.getMessage());
                        // Note: L'erreur est loggée, mais le message n'est pas mis dans dead letters ici
                        // car sendTell() est asynchrone. La gestion des dead letters se fait dans MessageService.
                    }
                );
    }
    
    /**
     * Envoie un ASK vers un autre microservice et retourne la réponse
     * CORRECTION: Ajout d'un timeout et meilleure gestion d'erreur
     */
    public CompletableFuture<String> sendAsk(@NonNull AskMessage msg, @NonNull String baseUrl) {
        System.out.println("[REMOTE ASK] ASK envoyé à " + baseUrl);
        // Note: originService sera défini par le MessageController du service distant
        return webClient.post()
                .uri(baseUrl + "/api/messages/ask")
                .bodyValue(msg)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(TIMEOUT)
                .doOnError(error -> System.err.println("[REMOTE ASK ERROR] Erreur lors de l'envoi vers " + baseUrl + ": " + error.getMessage()))
                .toFuture();
    }
}
