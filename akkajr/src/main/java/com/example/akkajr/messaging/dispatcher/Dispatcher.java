package com.example.akkajr.messaging.dispatcher;

import com.example.akkajr.messaging.AskMessage;
import com.example.akkajr.messaging.Message;
import com.example.akkajr.messaging.MessageService;
import com.example.akkajr.messaging.mailbox.DeadLetterMailbox;
import com.example.akkajr.messaging.mailbox.Mailbox;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    
        // For ASK messages, we need to deliver them so the actor can reply
        // The MessageService has already blocked the receiver
        if (msg instanceof AskMessage) {
            System.out.println("[ASK DELIVERED] " + msg.getReceiverId() + " a reçu l'ASK : " + msg.getContent());
            // The message is already in the mailbox, it will be processed
            // But we need to ensure actors can access it
            return;
        }
        
        // TELL : traitement asynchrone
        executor.submit(() -> {
            System.out.println("[TELL] " + msg.getContent());
        });
    }
}
