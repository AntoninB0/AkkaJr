package com.example.akkajr.core.actors;

public class NotificationActor extends Actor {
    
    @Override
    public void receive(Object message, ActorRef sender) {
        if (message instanceof SendNotification cmd) {
            logger.info("Notification envoyée à " + cmd.recipient + ": " + cmd.content);
            sender.tell(new NotificationSent(cmd.recipient, true), getContext().getSelf());
        }
    }
    
    public record SendNotification(String recipient, String content) {}
    public record NotificationSent(String recipient, boolean success) {}
}