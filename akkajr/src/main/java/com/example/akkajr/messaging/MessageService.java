package com.example.akkajr.messaging;

import com.example.akkajr.messaging.dispatcher.Dispatcher;
import com.example.akkajr.messaging.mailbox.DeadLetterMailbox;
import com.example.akkajr.messaging.mailbox.Mailbox;
import com.example.akkajr.messaging.mailbox.MessageLog;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MessageService {

    private final Map<String, Mailbox> mailboxes = new ConcurrentHashMap<>();
    private final DeadLetterMailbox deadLetters = new DeadLetterMailbox();
    private final MessageLog history = new MessageLog();
    private final Dispatcher dispatcher = new Dispatcher(deadLetters, this);

    // Map pour suivre les ASK reçus non encore répondus
    private final Map<String, AskMessage> pendingReceivedAsks = new ConcurrentHashMap<>();

    // Envoi d’un message (TELL ou ASK)
    public void send(Message msg) {

        // Bloquer l’expéditeur si il a un ASK non répondu
        if (pendingReceivedAsks.containsKey(msg.getSenderId())) {
            System.out.println("[BLOCKED] " + msg.getSenderId() +
                    " ne peut pas envoyer de message tant qu'il n'a pas répondu à son ASK.");
            return;
        }

        // Log de tous les messages
        history.log(msg);

        // Récupérer ou créer la mailbox du destinataire
        Mailbox target = mailboxes.computeIfAbsent(msg.getReceiverId(), id -> new Mailbox());

        // Si c’est un ASK, bloquer immédiatement le destinataire
        if (msg instanceof AskMessage ask) {
            pendingReceivedAsks.put(ask.getReceiverId(), ask);
            System.out.println("[ASK SYNC] " + ask.getReceiverId() + " est maintenant bloqué pour répondre à l'ASK");
        }

        // Ajouter le message à la mailbox
        target.enqueue(msg);

        // Dispatcher pour TELL asynchrone
        dispatcher.dispatch(msg, target);
    }

    // Inbox consultable sans vider la mailbox
    public Queue<Message> inbox(String agentId) {
        return mailboxes.getOrDefault(agentId, new Mailbox()).queueCopy();
    }

    // Historique complet
    public Queue<Message> history() {
        return history.getAll();
    }

    // Dead letters
    public Queue<Message> getDeadLetters() {
        return deadLetters.getAll();
    }

    // Répondre à un ASK et débloquer l’agent
    public void replyToAsk(String agentId, String responseContent) {
        AskMessage ask = pendingReceivedAsks.get(agentId);
        if (ask != null) {
            ask.complete(responseContent);
            pendingReceivedAsks.remove(agentId);
            System.out.println("[REPLY] " + agentId + " a répondu à l'ASK : " + responseContent);

            // Supprimer l'ASK de la mailbox pour libérer l’inbox
            Mailbox mailbox = mailboxes.get(agentId);
            if (mailbox != null) {
                mailbox.remove(ask);
            }
        }
    }
}
