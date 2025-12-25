package com.example.akkajr.messaging;

import com.example.akkajr.messaging.dispatcher.Dispatcher;
import com.example.akkajr.messaging.mailbox.DeadLetterMailbox;
import com.example.akkajr.messaging.mailbox.Mailbox;
import com.example.akkajr.messaging.mailbox.MessageLog;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MessageService {

    private final Map<String, Mailbox> mailboxes = new ConcurrentHashMap<>();
    private final DeadLetterMailbox deadLetters = new DeadLetterMailbox();
    private final MessageLog history = new MessageLog();
    private final Dispatcher dispatcher = new Dispatcher(deadLetters, this);
    
    @Autowired(required = false)
    private RemoteMessageClient remoteMessageClient;
    
    @Value("${app.service.name:akkajr}")
    private String currentServiceName;
    
    @Value("${app.remote.services:}")
    private String remoteServicesConfig; // Format: "service1=http://localhost:8081,service2=http://localhost:8082"

    // Map pour suivre les ASK reçus non encore répondus
    private final Map<String, AskMessage> pendingReceivedAsks = new ConcurrentHashMap<>();

    // Map pour mapper les noms de services aux URLs
    private Map<String, String> remoteServiceUrls;

    public MessageService() {
        // Initialiser les URLs des services distants
        this.remoteServiceUrls = new ConcurrentHashMap<>();
    }
    
    // Méthode appelée après injection des dépendances
    @PostConstruct
    public void init() {
        if (remoteServicesConfig != null && !remoteServicesConfig.isBlank()) {
            String[] services = remoteServicesConfig.split(",");
            for (String service : services) {
                String[] parts = service.trim().split("=");
                if (parts.length == 2) {
                    remoteServiceUrls.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        System.out.println("[MessageService] Service actuel: " + currentServiceName);
        System.out.println("[MessageService] Services distants configurés: " + remoteServiceUrls);
        System.out.println("[MessageService] RemoteMessageClient disponible: " + (remoteMessageClient != null));
    }

    // Envoi d'un message (TELL ou ASK)
    public void send(Message msg) {
        String receiverId = msg.getReceiverId();
        
        // Parser l'adresse pour détecter si c'est local ou remote
        AgentAddress receiverAddress = AgentAddress.parse(receiverId);
        
        // DEBUG: Vérifier la détection remote
        System.out.println("[DEBUG REMOTE] receiverId=" + receiverId);
        System.out.println("[DEBUG REMOTE] receiverAddress.isRemote()=" + receiverAddress.isRemote());
        System.out.println("[DEBUG REMOTE] remoteMessageClient != null=" + (remoteMessageClient != null));
        System.out.println("[DEBUG REMOTE] remoteServiceUrls=" + remoteServiceUrls);
        
        // Si c'est un message remote, utiliser RemoteMessageClient
        if (receiverAddress.isRemote() && remoteMessageClient != null) {
            String remoteUrl = remoteServiceUrls.get(receiverAddress.serviceName());
            System.out.println("[DEBUG REMOTE] remoteUrl pour " + receiverAddress.serviceName() + " = " + remoteUrl);
            
            if (remoteUrl == null) {
                System.err.println("[ERROR] Service distant inconnu: " + receiverAddress.serviceName());
                deadLetters.push(msg);
                return;
            }
            
            System.out.println("[REMOTE] Envoi vers " + receiverAddress.serviceName() + " (" + remoteUrl + ")");
            
            if (msg instanceof AskMessage ask) {
<<<<<<< HEAD
                remoteMessageClient.sendAsk(ask, remoteUrl);
            } else {
=======
                // CORRECTION: Lier le CompletableFuture retourné au futureResponse de l'AskMessage
                CompletableFuture<String> remoteFuture = remoteMessageClient.sendAsk(ask, remoteUrl);
                remoteFuture.whenComplete((response, error) -> {
                    if (error != null) {
                        System.err.println("[REMOTE ASK ERROR] Erreur lors de l'envoi ASK vers " + remoteUrl + ": " + error.getMessage());
                        ask.getFutureResponse().completeExceptionally(error);
                        // Mettre dans dead letters en cas d'erreur
                        deadLetters.push(ask);
                    } else {
                        System.out.println("[REMOTE ASK SUCCESS] Réponse reçue: " + response);
                        ask.complete(response);
                    }
                });
            } else {
                // Pour TELL, l'envoi est asynchrone, la gestion d'erreur se fait dans RemoteMessageClient
                // Si une erreur critique survient, elle sera loggée dans RemoteMessageClient
>>>>>>> pre-merge
                remoteMessageClient.sendTell(msg, remoteUrl);
            }
            return;
        }
        
        // Sinon, traitement local (code existant)
        String localReceiverId = receiverAddress.agentId();

        // DEBUG: Log pour voir ce qui arrive
        System.out.println("[DEBUG] Message reçu: " + msg.getClass().getSimpleName() + 
                          " de " + msg.getSenderId() + " vers " + localReceiverId);
        System.out.println("[DEBUG] " + localReceiverId + " bloqué? " + pendingReceivedAsks.containsKey(localReceiverId));

        // CORRECTION: Bloquer le RECEIVER s'il a un ASK non répondu
        if (pendingReceivedAsks.containsKey(localReceiverId)) {
            System.out.println("[BLOCKED] " + localReceiverId + 
                    " ne peut pas recevoir de message tant qu'il n'a pas répondu à son ASK.");
            // Mettre dans dead letters au lieu de l'accepter
            deadLetters.push(msg);
            return;
        }

        // Log de tous les messages
        history.log(msg);

        // Récupérer ou créer la mailbox du destinataire
        Mailbox target = mailboxes.computeIfAbsent(localReceiverId, id -> new Mailbox());

        // Si c'est un ASK, bloquer immédiatement le destinataire
<<<<<<< HEAD
        if (msg instanceof AskMessage ask) {
            pendingReceivedAsks.put(ask.getReceiverId(), ask);
            System.out.println("[ASK SYNC] " + ask.getReceiverId() + 
=======
        // CORRECTION: Utiliser localReceiverId au lieu de ask.getReceiverId() pour éviter les incohérences
        if (msg instanceof AskMessage ask) {
            pendingReceivedAsks.put(localReceiverId, ask);
            System.out.println("[ASK SYNC] " + localReceiverId + 
>>>>>>> pre-merge
                    " est maintenant bloqué pour répondre à l'ASK");
            System.out.println("[DEBUG] pendingReceivedAsks contient maintenant: " + pendingReceivedAsks.keySet());
        } else {
            System.out.println("[DEBUG] Ce n'est PAS un AskMessage, c'est: " + msg.getClass().getName());
        }

        // Ajouter le message à la mailbox
        target.enqueue(msg);

        // Dispatcher pour TELL asynchrone
        dispatcher.dispatch(msg, target);
    }

    // Inbox consultable sans vider la mailbox
    public Queue<Message> inbox(String agentId) {
        // Parser pour gérer les adresses remote
        AgentAddress address = AgentAddress.parse(agentId);
        if (address.isRemote()) {
            // Pour les agents remote, on ne peut pas consulter l'inbox directement
            return new java.util.concurrent.ConcurrentLinkedQueue<>();
        }
        return mailboxes.getOrDefault(address.agentId(), new Mailbox()).queueCopy();
    }

    // Historique complet
    public Queue<Message> history() {
        return history.getAll();
    }

    // Dead letters
    public Queue<Message> getDeadLetters() {
        return deadLetters.getAll();
    }

    // Répondre à un ASK et débloquer l'agent
    public void replyToAsk(String agentId, String responseContent) {
        AgentAddress address = AgentAddress.parse(agentId);
        String localAgentId = address.agentId();
        
        AskMessage ask = pendingReceivedAsks.get(localAgentId);
        if (ask != null) {
            ask.complete(responseContent);
            pendingReceivedAsks.remove(localAgentId);
            System.out.println("[REPLY] " + localAgentId + " a répondu à l'ASK : " + responseContent);

            // Supprimer l'ASK de la mailbox pour libérer l'inbox
            Mailbox mailbox = mailboxes.get(localAgentId);
            if (mailbox != null) {
                mailbox.remove(ask);
            }
        } else {
            System.out.println("[WARNING] Tentative de réponse à un ASK inexistant pour : " + localAgentId);
        }
    }
    
    // Nouvelle méthode pour vérifier si un acteur est bloqué
    public boolean isBlocked(String agentId) {
        AgentAddress address = AgentAddress.parse(agentId);
        return pendingReceivedAsks.containsKey(address.agentId());
    }
    
    // Nouvelle méthode pour obtenir l'ASK en attente
    public AskMessage getPendingAsk(String agentId) {
        AgentAddress address = AgentAddress.parse(agentId);
        return pendingReceivedAsks.get(address.agentId());
    }
}
