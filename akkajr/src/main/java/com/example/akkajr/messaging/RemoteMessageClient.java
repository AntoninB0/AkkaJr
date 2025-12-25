package com.example.akkajr.messaging;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

<<<<<<< HEAD
=======
import java.time.Duration;
>>>>>>> pre-merge
import java.util.concurrent.CompletableFuture;

@Service
public class RemoteMessageClient {
    
    private final WebClient webClient;
<<<<<<< HEAD
=======
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
>>>>>>> pre-merge
    
    public RemoteMessageClient() {
        this.webClient = WebClient.builder()
            .build();
    }
    
    /**
     * Envoie un TELL vers un autre microservice
<<<<<<< HEAD
=======
     * CORRECTION: Amélioration de la gestion d'erreur avec timeout
>>>>>>> pre-merge
     */
    public void sendTell(@NonNull Message msg, @NonNull String baseUrl) {
        System.out.println("[REMOTE TELL] Message envoyé à " + baseUrl);
        webClient.post()
                .uri(baseUrl + "/api/messages/tell")
                .bodyValue(msg)
                .retrieve()
                .bodyToMono(String.class)
<<<<<<< HEAD
                .subscribe(
                    result -> System.out.println("[REMOTE TELL] Réponse: " + result),
                    error -> System.err.println("[REMOTE TELL] Erreur: " + error.getMessage())
=======
                .timeout(TIMEOUT)
                .subscribe(
                    result -> System.out.println("[REMOTE TELL] Réponse: " + result),
                    error -> {
                        System.err.println("[REMOTE TELL ERROR] Erreur lors de l'envoi vers " + baseUrl + ": " + error.getMessage());
                        // Note: L'erreur est loggée, mais le message n'est pas mis dans dead letters ici
                        // car sendTell() est asynchrone. La gestion des dead letters se fait dans MessageService.
                    }
>>>>>>> pre-merge
                );
    }
    
    /**
     * Envoie un ASK vers un autre microservice et retourne la réponse
<<<<<<< HEAD
=======
     * CORRECTION: Ajout d'un timeout et meilleure gestion d'erreur
>>>>>>> pre-merge
     */
    public CompletableFuture<String> sendAsk(@NonNull AskMessage msg, @NonNull String baseUrl) {
        System.out.println("[REMOTE ASK] ASK envoyé à " + baseUrl);
        // Note: originService sera défini par le MessageController du service distant
        return webClient.post()
                .uri(baseUrl + "/api/messages/ask")
                .bodyValue(msg)
                .retrieve()
                .bodyToMono(String.class)
<<<<<<< HEAD
=======
                .timeout(TIMEOUT)
                .doOnError(error -> System.err.println("[REMOTE ASK ERROR] Erreur lors de l'envoi vers " + baseUrl + ": " + error.getMessage()))
>>>>>>> pre-merge
                .toFuture();
    }
}
