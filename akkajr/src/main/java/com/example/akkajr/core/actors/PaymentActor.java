package com.example.akkajr.core.actors;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PaymentActor extends Actor {
    
    private final Map<String, Payment> payments = new ConcurrentHashMap<>();
    private ActorRef notificationActor;
    
    @Override
    public void preStart() {
        logger.info("PaymentActor started");
    }
    
    @Override
    public void receive(Object message, ActorRef sender) {
        if (message instanceof SetNotificationActor set) {
            this.notificationActor = set.actorRef;
            logger.info("NotificationActor registered in PaymentActor");
            
        } else if (message instanceof ProcessPayment cmd) {
            String paymentId = UUID.randomUUID().toString();
            
            // Simulate payment processing
            boolean success = cmd.amount > 0 && cmd.amount < 10000;
            String status = success ? "SUCCESS" : "FAILED";
            
            Payment payment = new Payment(paymentId, cmd.orderId, cmd.amount, status);
            payments.put(paymentId, payment);
            
            logger.info("Payment " + status + ": " + paymentId + " (" + cmd.amount + " EUR)");
            
            // COMMUNICATION ASYNCHRONE 1: Réponse à OrderActor
            sender.tell(payment, getContext().getSelf());
            
            // COMMUNICATION ASYNCHRONE 2: Vers NotificationActor
            if (notificationActor != null && success) {
                logger.info("Sending notification for order: " + cmd.orderId);
                
                // tell() = ASYNCHRONE
                notificationActor.tell(
                    new NotificationActor.SendNotification(
                        "customer@email.com", 
                        "Payment confirmed for order " + cmd.orderId + " (" + cmd.amount + " EUR)"
                    ),
                    getContext().getSelf()  // sender = PaymentActor
                );
            }
        }
    }
    
    public record ProcessPayment(String orderId, double amount) {}
    public record SetNotificationActor(ActorRef actorRef) {}
    
    public static class Payment {
        public String id;
        public String orderId;
        public double amount;
        public String status;
        
        public Payment(String id, String orderId, double amount, String status) {
            this.id = id;
            this.orderId = orderId;
            this.amount = amount;
            this.status = status;
        }
    }
}