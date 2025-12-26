package com.example.akkajr.messaging.mailbox;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.example.akkajr.messaging.Message;

public class DeadLetterMailbox {

    private final Queue<Message> deadLetters = new ConcurrentLinkedQueue<>();

    public void push(Message m) {
        deadLetters.offer(m);
    }

    public Queue<Message> getAll() {
        return new ConcurrentLinkedQueue<>(deadLetters);
    }

    public boolean isEmpty() {
        return deadLetters.isEmpty();
    }

    public int size() {
        return deadLetters.size();
    }
}
