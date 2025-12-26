package com.example.akkajr.flightradar;

import com.example.akkajr.core.actors.ActorRef;
import com.example.akkajr.core.actors.ActorSystem;
import com.example.akkajr.core.actors.Props;
import com.example.akkajr.messaging.AskMessage;
import com.example.akkajr.messaging.Message;
import com.example.akkajr.messaging.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * REST Controller for Flight Tracker Service (Service 1)
 */
@RestController
@RequestMapping("/api/flights")
public class FlightTrackerController {
    
    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("coreActorSystem")
    private ActorSystem actorSystem;
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private FlightRadarClient flightRadarClient;
    
    @Value("${app.service.name:service1}")
    private String currentServiceName;
    
    private ActorRef flightTracker;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @PostConstruct
    public void init() {
        // Create FlightTrackerActor with dependencies
        flightTracker = actorSystem.actorOf(
            Props.create(FlightTrackerActor.class, 
                        flightRadarClient, 
                        messageService, 
                        currentServiceName), 
            "flightTracker"
        );
        System.out.println("[FlightTrackerController] FlightTrackerActor initialized");
    }
    
    @PostMapping("/track")
    public ResponseEntity<?> trackFlight(@RequestBody TrackFlightRequest request) {
        try {
            String agentId = currentServiceName + ":flightTracker";
            Message msg = new Message(
                "client",
                agentId,
                objectMapper.writeValueAsString(
                    Map.of("action", "track", "flightId", request.flightId())
                )
            );
            messageService.send(msg);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Flight tracking initiated for " + request.flightId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/status/{flightId}")
    public ResponseEntity<?> getFlightStatus(@PathVariable String flightId) {
        try {
            String agentId = currentServiceName + ":flightTracker";
            AskMessage askMsg = new AskMessage(
                "client",
                agentId,
                objectMapper.writeValueAsString(
                    Map.of("action", "status", "flightId", flightId)
                )
            );
            
            // Send the message (returns void)
            messageService.send(askMsg);
            
            // Get the CompletableFuture from the AskMessage itself
            CompletableFuture<String> future = askMsg.getFutureResponse();
            String response = future.get(10, TimeUnit.SECONDS);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Error getting flight status: " + e.getMessage()
            ));
        }
    }
    
    public record TrackFlightRequest(String flightId) {}
}