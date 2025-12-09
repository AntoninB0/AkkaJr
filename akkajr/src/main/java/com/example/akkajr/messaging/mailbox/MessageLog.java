package com.example.akkajr.messaging.mailbox;

import com.example.akkajr.messaging.Message;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageLog {

    private final Queue<Message> history = new ConcurrentLinkedQueue<>();

    public void log(Message m) {
        history.offer(m);
    }

    public Queue<Message> getAll() {
        return new ConcurrentLinkedQueue<>(history);
    }
}
