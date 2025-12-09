package com.example.akkajr.controllers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.akkajr.core.actors.Actor;
import com.example.akkajr.core.actors.ActorRef;
import com.example.akkajr.core.actors.ActorSystem;
import com.example.akkajr.core.actors.NotificationActor;
import com.example.akkajr.core.actors.OrderActor;
import com.example.akkajr.core.actors.PaymentActor;
import com.example.akkajr.core.actors.Props;
import com.example.akkajr.core.actors.SupervisorActor;

import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/api/actors")
public class ActorController {
    
    @Autowired
    private ActorSystem actorSystem;
    
    private ActorRef supervisorRef;
    
    @PostConstruct
    public void init() {
        supervisorRef = actorSystem.actorOf(Props.create(SupervisorActor.class), "supervisor");
    }
    
    private ActorRef getSupervisor() {
        return supervisorRef;
    }
    
    @PostMapping("/order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body) throws Exception {
        List<String> items = (List<String>) body.get("items");
        
        ActorRef supervisor = getSupervisor();
        
        CompletableFuture<Object> future = ask(supervisor, 
            new SupervisorActor.RouteMessage("order", new OrderActor.CreateOrder(items)));
        
        Object result = future.get(5, TimeUnit.SECONDS);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/payment")
    public ResponseEntity<?> processPayment(@RequestBody Map<String, Object> body) throws Exception {
        String orderId = (String) body.get("orderId");
        double amount = ((Number) body.get("amount")).doubleValue();
        
        ActorRef supervisor = getSupervisor();
        
        CompletableFuture<Object> future = ask(supervisor,
            new SupervisorActor.RouteMessage("payment", new PaymentActor.ProcessPayment(orderId, amount)));
        
        Object result = future.get(5, TimeUnit.SECONDS);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/notify")
    public ResponseEntity<?> sendNotification(@RequestBody Map<String, Object> body) throws Exception {
        String recipient = (String) body.get("recipient");
        String content = (String) body.get("content");
        
        ActorRef supervisor = getSupervisor();
        
        CompletableFuture<Object> future = ask(supervisor,
            new SupervisorActor.RouteMessage("notification", 
                new NotificationActor.SendNotification(recipient, content)));
        
        Object result = future.get(5, TimeUnit.SECONDS);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/init")
    public ResponseEntity<?> initActors() throws Exception {
        ActorRef supervisor = getSupervisor();
        
        if (supervisor == null) {
            return ResponseEntity.status(500).body(Map.of("error", "Supervisor not initialized"));
        }
        
        ask(supervisor, new SupervisorActor.CreateChildRequest(
            Props.create(OrderActor.class), "order")).get();
        ask(supervisor, new SupervisorActor.CreateChildRequest(
            Props.create(PaymentActor.class), "payment")).get();
        ask(supervisor, new SupervisorActor.CreateChildRequest(
            Props.create(NotificationActor.class), "notification")).get();
        
        CompletableFuture<Object> paymentFuture = ask(supervisor, new SupervisorActor.GetChildRef("payment"));
        ActorRef paymentRef = (ActorRef) paymentFuture.get(2, TimeUnit.SECONDS);
        
        CompletableFuture<Object> notifFuture = ask(supervisor, new SupervisorActor.GetChildRef("notification"));
        ActorRef notifRef = (ActorRef) notifFuture.get(2, TimeUnit.SECONDS);
        
        supervisor.tell(new SupervisorActor.RouteMessage("order", 
            new OrderActor.SetPaymentActor(paymentRef)), null);
        
        supervisor.tell(new SupervisorActor.RouteMessage("payment",
            new PaymentActor.SetNotificationActor(notifRef)), null);
        
        return ResponseEntity.ok(Map.of(
            "message", "Acteurs initialisés et connectés", 
            "services", List.of("order", "payment", "notification"),
            "communication", "asynchrone"
        ));
    }
    
    @GetMapping("/list")
    public ResponseEntity<?> listActors() throws Exception {
        ActorRef supervisor = getSupervisor();
        CompletableFuture<Object> future = ask(supervisor, new SupervisorActor.GetChildrenRequest());
        Object result = future.get(2, TimeUnit.SECONDS);
        return ResponseEntity.ok(result);
    }
    
    private CompletableFuture<Object> ask(ActorRef actor, Object message) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        
        ActorRef tempActor = actorSystem.actorOf(Props.create(TempActor.class, future));
        actor.tell(message, tempActor);
        
        return future;
    }
    
    public static class TempActor extends Actor {
        private final CompletableFuture<Object> future;
        
        public TempActor(CompletableFuture<Object> future) {
            this.future = future;
        }
        
        @Override
        public void receive(Object message, ActorRef sender) {
            future.complete(message);
            getContext().getSystem().stop(getContext().getSelf());
        }
    }
}