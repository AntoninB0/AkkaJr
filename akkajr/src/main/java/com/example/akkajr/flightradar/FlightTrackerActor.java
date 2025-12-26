package com.example.akkajr.flightradar;

import com.example.akkajr.core.actors.Actor;
import com.example.akkajr.core.actors.ActorRef;
import com.example.akkajr.messaging.Message;
import com.example.akkajr.messaging.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Actor that tracks flights by querying Flight Radar API
 * Runs in Service 1 (Flight Tracker Service)
 */
public class FlightTrackerActor extends Actor {
    private static final Logger logger = LoggerFactory.getLogger(FlightTrackerActor.class);
    
    private FlightRadarClient flightRadarClient;
    private MessageService messageService;
    private String currentServiceName;
    private ObjectMapper objectMapper;
    
    // Constructor for dependency injection via Props
    public FlightTrackerActor(FlightRadarClient flightRadarClient, 
                             MessageService messageService,
                             String currentServiceName) {
        this.flightRadarClient = flightRadarClient;
        this.messageService = messageService;
        this.currentServiceName = currentServiceName;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public void preStart() throws Exception {
        logger.info("FlightTrackerActor started in service: {}", currentServiceName);
    }
    
    @Override
    public void receive(Object message, ActorRef sender) throws Exception {
        try {
            if (message instanceof TrackFlightRequest req) {
                handleTrackFlight(req, sender);
            } else if (message instanceof GetFlightStatusRequest req) {
                handleGetFlightStatus(req, sender);
            } else if (message instanceof String) {
                // Try to parse as JSON request
                try {
                    JsonRequest jsonReq = objectMapper.readValue((String) message, JsonRequest.class);
                    if ("track".equals(jsonReq.action)) {
                        handleTrackFlight(new TrackFlightRequest(jsonReq.flightId), sender);
                    } else if ("status".equals(jsonReq.action)) {
                        handleGetFlightStatus(new GetFlightStatusRequest(jsonReq.flightId), sender);
                    }
                } catch (Exception e) {
                    logger.warn("Could not parse message as JSON: {}", message);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage(), e);
            sender.tell(new ErrorResponse("Error: " + e.getMessage()), getContext().getSelf());
        }
    }
    
    private void handleTrackFlight(TrackFlightRequest req, ActorRef sender) {
        logger.info("Tracking flight: {}", req.flightId());
        
        // Query Flight Radar API asynchronously
        flightRadarClient.getFlightData(req.flightId())
            .subscribe(
                flightData -> {
                    try {
                        // Send update to monitor service via remote message
                        String monitorAgent = "service2:flightMonitor";
                        Message updateMsg = new Message(
                            currentServiceName + ":flightTracker",
                            monitorAgent,
                            objectMapper.writeValueAsString(flightData)
                        );
                        updateMsg.setOriginService(currentServiceName);
                        messageService.send(updateMsg);
                        
                        logger.info("Flight data sent to monitor service: {}", flightData.getFlightId());
                        
                        // Reply to sender
                        TrackFlightResponse response = new TrackFlightResponse(
                            true, 
                            "Flight tracked: " + req.flightId() + 
                            " - Position: " + flightData.getLatitude() + "," + flightData.getLongitude()
                        );
                        sender.tell(response, getContext().getSelf());
                    } catch (Exception e) {
                        logger.error("Error sending flight update: {}", e.getMessage());
                        sender.tell(new ErrorResponse("Error sending update: " + e.getMessage()), 
                                   getContext().getSelf());
                    }
                },
                error -> {
                    logger.error("Error tracking flight {}: {}", req.flightId(), error.getMessage());
                    sender.tell(new TrackFlightResponse(false, 
                        "Error tracking flight: " + error.getMessage()), 
                        getContext().getSelf());
                }
            );
    }
    
    private void handleGetFlightStatus(GetFlightStatusRequest req, ActorRef sender) {
        logger.info("Getting status for flight: {}", req.flightId());
        
        // Query current status
        flightRadarClient.getFlightData(req.flightId())
            .subscribe(
                flightData -> {
                    try {
                        sender.tell(objectMapper.writeValueAsString(flightData), getContext().getSelf());
                    } catch (Exception e) {
                        sender.tell(new ErrorResponse("Error serializing flight data: " + e.getMessage()), 
                                   getContext().getSelf());
                    }
                },
                error -> {
                    logger.error("Error getting flight status: {}", error.getMessage());
                    sender.tell(new ErrorResponse("Error: " + error.getMessage()), 
                               getContext().getSelf());
                }
            );
    }
    
    // Message types
    public record TrackFlightRequest(String flightId) {}
    public record TrackFlightResponse(boolean success, String message) {}
    public record GetFlightStatusRequest(String flightId) {}
    public record ErrorResponse(String error) {}
    
    // Helper class for JSON parsing
    private static class JsonRequest {
        public String action;
        public String flightId;
    }
}