package com.example.akkajr.core.actors;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OrderActor extends Actor {
    
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private ActorRef paymentActor;
    
    @Override
    public void preStart() {
        logger.info("OrderActor started");
    }
    
    @Override
    public void receive(Object message, ActorRef sender) {
        if (message instanceof SetPaymentActor set) {
            this.paymentActor = set.actorRef;
            logger.info("PaymentActor registered in OrderActor");
            
        } else if (message instanceof CreateOrder cmd) {
            Order order = new Order(UUID.randomUUID().toString(), cmd.items, "PENDING");
            orders.put(order.id, order);
            logger.info("Order created: " + order.id);
            
            // COMMUNICATION ASYNCHRONE vers PaymentActor
            if (paymentActor != null) {
                double total = order.items.size() * 99.99;
                logger.info("Sending payment request: " + total + " EUR for order " + order.id);
                
                // tell() = ASYNCHRONE (non-bloquant)
                paymentActor.tell(
                    new PaymentActor.ProcessPayment(order.id, total), 
                    getContext().getSelf()  // sender = OrderActor
                );
            }
            
            sender.tell(order, getContext().getSelf());
            
        } else if (message instanceof PaymentActor.Payment payment) {
            // RECEPTION ASYNCHRONE de la réponse du PaymentActor
            logger.info("Payment received for order: " + payment.orderId + " - Status: " + payment.status);
            
            Order order = orders.get(payment.orderId);
            if (order != null) {
                order.status = payment.status.equals("SUCCESS") ? "PAID" : "PAYMENT_FAILED";
                logger.info("Order updated: " + order.id + " -> " + order.status);
            }
            
        } else if (message instanceof GetOrder query) {
            Order order = orders.get(query.orderId);
            sender.tell(order != null ? order : new ErrorResponse("Order not found"), getContext().getSelf());
        }
    }
    
    public record CreateOrder(List<String> items) {}
    public record GetOrder(String orderId) {}
    public record SetPaymentActor(ActorRef actorRef) {}
    public record ErrorResponse(String error) {}
    
    public static class Order {
        public String id;
        public List<String> items;
        public String status;
        
        public Order(String id, List<String> items, String status) {
            this.id = id;
            this.items = items;
            this.status = status;
        }
    }
}