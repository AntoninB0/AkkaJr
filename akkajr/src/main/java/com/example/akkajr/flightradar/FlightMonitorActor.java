package com.example.akkajr.flightradar;

import com.example.akkajr.core.actors.Actor;
import com.example.akkajr.core.actors.ActorRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.akkajr.messaging.AskMessage;
import com.example.akkajr.messaging.MessageService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Actor that monitors and aggregates flight data from tracker service
 * Runs in Service 2 (Flight Monitor Service)
 */
public class FlightMonitorActor extends Actor {
    private static final Logger logger = LoggerFactory.getLogger(FlightMonitorActor.class);
    
    private final Map<String, FlightData> trackedFlights = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private MessageService messageService;
    private String currentServiceName;
    
    public FlightMonitorActor() {
        this.objectMapper = new ObjectMapper();
    }
    
    // Constructor with MessageService dependency
    public FlightMonitorActor(MessageService messageService, String currentServiceName) {
        this.objectMapper = new ObjectMapper();
        this.messageService = messageService;
        this.currentServiceName = currentServiceName;
    }
    
    @Override
    public void preStart() throws Exception {
        logger.info("FlightMonitorActor started");
    }
    
    @Override
    public void receive(Object message, ActorRef sender) throws Exception {
        try {
            // Check if this is an AskMessage from MessageService
            if (message instanceof AskMessage askMsg) {
                handleAskMessage(askMsg);
                return;
            }
            
            if (message instanceof String) {
                // Parse flight data update from tracker service
                handleFlightUpdate((String) message, sender);
            } else if (message instanceof GetTrackedFlightsRequest) {
                GetTrackedFlightsResponse response = new GetTrackedFlightsResponse(
                    Map.copyOf(trackedFlights)
                );
                sender.tell(objectMapper.writeValueAsString(response), getContext().getSelf());
            } else if (message instanceof GetFlightDetailsRequest req) {
                FlightData data = trackedFlights.get(req.flightId());
                if (data != null) {
                    sender.tell(objectMapper.writeValueAsString(data), getContext().getSelf());
                } else {
                    sender.tell(objectMapper.writeValueAsString(
                        new ErrorResponse("Flight not found: " + req.flightId())), 
                        getContext().getSelf());
                }
            } else {
                // Try to parse as JSON (for non-ASK messages, use sender.tell)
                try {
                    JsonRequest jsonReq = objectMapper.readValue(message.toString(), JsonRequest.class);
                    if ("list".equals(jsonReq.action)) {
                        GetTrackedFlightsResponse response = new GetTrackedFlightsResponse(
                            Map.copyOf(trackedFlights)
                        );
                        String responseJson = objectMapper.writeValueAsString(response);
                        sender.tell(responseJson, getContext().getSelf());
                    } else if ("details".equals(jsonReq.action)) {
                        FlightData data = trackedFlights.get(jsonReq.flightId);
                        if (data != null) {
                            String responseJson = objectMapper.writeValueAsString(data);
                            sender.tell(responseJson, getContext().getSelf());
                        } else {
                            String errorJson = objectMapper.writeValueAsString(
                                new ErrorResponse("Flight not found"));
                            sender.tell(errorJson, getContext().getSelf());
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not parse message: {}", message);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage(), e);
            if (message instanceof AskMessage askMsg) {
                replyToAsk(askMsg, objectMapper.writeValueAsString(
                    new ErrorResponse("Error: " + e.getMessage())));
            } else {
                sender.tell(objectMapper.writeValueAsString(
                    new ErrorResponse("Error: " + e.getMessage())), getContext().getSelf());
            }
        }
    }
    
    private void handleAskMessage(AskMessage askMsg) {
        try {
            String content = askMsg.getContent();
            JsonRequest jsonReq = objectMapper.readValue(content, JsonRequest.class);
            
            if ("list".equals(jsonReq.action)) {
                GetTrackedFlightsResponse response = new GetTrackedFlightsResponse(
                    Map.copyOf(trackedFlights)
                );
                String responseJson = objectMapper.writeValueAsString(response);
                replyToAsk(askMsg, responseJson);
            } else if ("details".equals(jsonReq.action)) {
                FlightData data = trackedFlights.get(jsonReq.flightId);
                if (data != null) {
                    String responseJson = objectMapper.writeValueAsString(data);
                    replyToAsk(askMsg, responseJson);
                } else {
                    String errorJson = objectMapper.writeValueAsString(
                        new ErrorResponse("Flight not found"));
                    replyToAsk(askMsg, errorJson);
                }
            } else {
                replyToAsk(askMsg, objectMapper.writeValueAsString(
                    new ErrorResponse("Unknown action: " + jsonReq.action)));
            }
        } catch (Exception e) {
            logger.error("Error handling ASK message: {}", e.getMessage(), e);
            try {
                replyToAsk(askMsg, objectMapper.writeValueAsString(
                    new ErrorResponse("Error processing request: " + e.getMessage())));
            } catch (Exception ex) {
                logger.error("Failed to send error response", ex);
            }
        }
    }
    
    private void replyToAsk(AskMessage askMsg, String response) {
        if (messageService != null) {
            String agentId = currentServiceName + ":flightMonitor";
            messageService.replyToAsk(agentId, response);
            logger.info("Replied to ASK: {}", response);
        } else {
            // Fallback: complete the future directly if MessageService not available
            askMsg.complete(response);
            logger.warn("MessageService not available, completed ASK directly");
        }
    }
    
    private void handleFlightUpdate(String updateData, ActorRef sender) {
        try {
            FlightData flightData = objectMapper.readValue(updateData, FlightData.class);
            trackedFlights.put(flightData.getFlightId(), flightData);
            logger.info("Received flight update: {} - Position: {},{}", 
                       flightData.getFlightId(), 
                       flightData.getLatitude(), 
                       flightData.getLongitude());
        } catch (Exception e) {
            logger.error("Error parsing flight update: {}", e.getMessage());
        }
    }
    
    // Message types
    public record GetTrackedFlightsRequest() {}
    public record GetTrackedFlightsResponse(Map<String, FlightData> flights) {}
    public record GetFlightDetailsRequest(String flightId) {}
    public record ErrorResponse(String error) {}
    
    // Helper class for JSON parsing
    private static class JsonRequest {
        public String action;
        public String flightId;
    }
}