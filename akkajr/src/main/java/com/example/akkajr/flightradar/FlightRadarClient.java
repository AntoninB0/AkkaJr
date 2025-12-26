package com.example.akkajr.flightradar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;

@Component
public class FlightRadarClient {
    private static final Logger logger = LoggerFactory.getLogger(FlightRadarClient.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private static final String FLIGHT_RADAR_API = "https://data-live.flightradar24.com";
    
    public FlightRadarClient() {
        this.webClient = WebClient.builder()
            .baseUrl(FLIGHT_RADAR_API)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Get flight data from Flight Radar API
     * Note: Flight Radar API requires specific bounds and returns data for all flights in that area
     * This is a simplified implementation that searches for a specific flight ID
     */
    public Mono<FlightData> getFlightData(String flightId) {
        // Flight Radar API endpoint - returns all flights in a bounding box
        // Bounds format: north,south,east,west (e.g., "56.84,71.6,-12.27,40.27" for Europe)
        String bounds = "56.84,71.6,-12.27,40.27"; // Europe bounds
        
        return webClient.get()
            .uri("/zones/fcgi/feed.js?bounds={bounds}&faa=1&mlat=1&flarm=1&adsb=1&gnd=1&air=1&vehicles=1&estimated=1&maxage=14400&gliders=1&stats=1",
                bounds)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(10))
            .map(json -> parseFlightData(json, flightId))
            .onErrorResume(e -> {
                logger.error("Error fetching flight data for {}: {}", flightId, e.getMessage());
                // Return mock data for demo purposes if API fails
                return Mono.just(createMockFlightData(flightId));
            });
    }
    
    /**
     * Parse Flight Radar API JSON response
     * Format: {"flightId": [lat, lon, heading, altitude, speed, ...]}
     */
    private FlightData parseFlightData(String json, String targetFlightId) {
        try {
            JsonNode root = objectMapper.readTree(json);
            
            // Flight Radar API returns a map where keys are flight IDs
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String flightId = entry.getKey();
                
                // Skip metadata fields
                if (flightId.equals("full_count") || flightId.equals("version")) {
                    continue;
                }
                
                // Check if this is the flight we're looking for (case-insensitive)
                if (flightId.equalsIgnoreCase(targetFlightId) || 
                    flightId.toUpperCase().contains(targetFlightId.toUpperCase())) {
                    JsonNode flightArray = entry.getValue();
                    
                    if (flightArray.isArray() && flightArray.size() >= 11) {
                        double lat = flightArray.get(1).asDouble();
                        double lon = flightArray.get(2).asDouble();
                        double heading = flightArray.get(3).asDouble();
                        double altitude = flightArray.get(4).asDouble();
                        double speed = flightArray.get(5).asDouble();
                        String origin = flightArray.size() > 11 ? flightArray.get(11).asText() : "N/A";
                        String destination = flightArray.size() > 12 ? flightArray.get(12).asText() : "N/A";
                        String airline = flightArray.size() > 13 ? flightArray.get(13).asText() : "N/A";
                        String aircraft = flightArray.size() > 8 ? flightArray.get(8).asText() : "N/A";
                        
                        FlightData data = new FlightData();
                        data.setFlightId(flightId);
                        data.setLatitude(lat);
                        data.setLongitude(lon);
                        data.setAltitude(altitude);
                        data.setSpeed(speed);
                        data.setOrigin(origin);
                        data.setDestination(destination);
                        data.setAirline(airline);
                        data.setAircraft(aircraft);
                        
                        logger.info("Found flight data for {}: lat={}, lon={}, alt={}", 
                                   flightId, lat, lon, altitude);
                        return data;
                    }
                }
            }
            
            // Flight not found in API response, return mock data
            logger.warn("Flight {} not found in API response, returning mock data", targetFlightId);
            return createMockFlightData(targetFlightId);
            
        } catch (Exception e) {
            logger.error("Error parsing flight data: {}", e.getMessage());
            return createMockFlightData(targetFlightId);
        }
    }
    
    /**
     * Create mock flight data for demo purposes
     */
    private FlightData createMockFlightData(String flightId) {
        FlightData data = new FlightData();
        data.setFlightId(flightId);
        data.setAirline("Demo Airline");
        data.setAircraft("Boeing 737");
        data.setLatitude(48.8566 + (Math.random() * 0.1)); // Paris area
        data.setLongitude(2.3522 + (Math.random() * 0.1));
        data.setAltitude(10000 + (Math.random() * 5000));
        data.setSpeed(800 + (Math.random() * 100));
        data.setOrigin("CDG");
        data.setDestination("JFK");
        return data;
    }
}