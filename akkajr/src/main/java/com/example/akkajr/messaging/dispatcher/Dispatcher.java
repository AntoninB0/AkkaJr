package com.example.akkajr.messaging.dispatcher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.akkajr.messaging.AskMessage;
import com.example.akkajr.messaging.Message;
import com.example.akkajr.messaging.MessageService;
import com.example.akkajr.messaging.mailbox.DeadLetterMailbox;
import com.example.akkajr.messaging.mailbox.Mailbox;

public class Dispatcher {

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final DeadLetterMailbox deadLetters;
    private final MessageService messageService;

    public Dispatcher(DeadLetterMailbox deadLetters, MessageService messageService) {
        this.deadLetters = deadLetters;
        this.messageService = messageService;
    }

    public void dispatch(Message msg, Mailbox targetMailbox) {
        if (targetMailbox == null) {
            deadLetters.push(msg);
            return;
        }

        // TELL : traitement asynchrone
        if (!(msg instanceof AskMessage)) {
            executor.submit(() -> {
                System.out.println("[TELL] " + msg.getContent());
            });
        } else {
            // ASK : déjà bloqué côté MessageService.send()
            System.out.println("[ASK DELIVERED] " + msg.getReceiverId() + " a reçu l'ASK : " + msg.getContent());
        }
    }
}
