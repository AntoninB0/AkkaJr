package com.example.akkajr.flightradar;

import com.example.akkajr.core.actors.ActorRef;
import com.example.akkajr.core.actors.ActorSystem;
import com.example.akkajr.core.actors.Props;
import com.example.akkajr.messaging.AskMessage;
import com.example.akkajr.messaging.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * REST Controller for Flight Monitor Service (Service 2)
 */
@RestController
@RequestMapping("/api/flights")
public class FlightMonitorController {
    
    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("coreActorSystem")
    private ActorSystem actorSystem;
    
    @Autowired
    private MessageService messageService;
    
    @Value("${app.service.name:service2}")
    private String currentServiceName;
    
    private ActorRef flightMonitor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @PostConstruct
    public void init() {
        // Create FlightMonitorActor with MessageService dependency
        flightMonitor = actorSystem.actorOf(
            Props.create(FlightMonitorActor.class, messageService, currentServiceName), 
            "flightMonitor"
        );
        System.out.println("[FlightMonitorController] FlightMonitorActor initialized");
    }
    
    @GetMapping("/tracked")
    public ResponseEntity<?> getTrackedFlights() {
        try {
            String agentId = currentServiceName + ":flightMonitor";
            AskMessage askMsg = new AskMessage(
                "client",
                agentId,
                objectMapper.writeValueAsString(Map.of("action", "list"))
            );
            
            // Send the message (returns void)
            messageService.send(askMsg);
            
            // Get the CompletableFuture from the AskMessage itself
            CompletableFuture<String> future = askMsg.getFutureResponse();
            String response = future.get(5, TimeUnit.SECONDS);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Error getting tracked flights: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/details/{flightId}")
    public ResponseEntity<?> getFlightDetails(@PathVariable String flightId) {
        try {
            String agentId = currentServiceName + ":flightMonitor";
            AskMessage askMsg = new AskMessage(
                "client",
                agentId,
                objectMapper.writeValueAsString(
                    Map.of("action", "details", "flightId", flightId)
                )
            );
            
            // Send the message (returns void)
            messageService.send(askMsg);
            
            // Get the CompletableFuture from the AskMessage itself
            CompletableFuture<String> future = askMsg.getFutureResponse();
            String response = future.get(5, TimeUnit.SECONDS);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Error getting flight details: " + e.getMessage()
            ));
        }
    }
}