package com.example.akkajr.messaging.mailbox;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.example.akkajr.messaging.Message;

public class Mailbox {

    private final Queue<Message> queue = new ConcurrentLinkedQueue<>();

    public void enqueue(Message message) {
        queue.offer(message);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public Queue<Message> queueCopy() {
        return new ConcurrentLinkedQueue<>(queue);
    }

    public void remove(Message m) {
        queue.remove(m);
    }
}
