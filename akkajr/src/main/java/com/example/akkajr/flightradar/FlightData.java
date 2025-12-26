package com.example.akkajr.flightradar;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FlightData {
    private String flightId;
    private String airline;
    private String aircraft;
    private double latitude;
    private double longitude;
    private double altitude;
    private double speed;
    private String origin;
    private String destination;
    private LocalDateTime lastUpdate;
    
    public FlightData() {
        this.lastUpdate = LocalDateTime.now();
    }
    
    public FlightData(String flightId, String airline, String aircraft, 
                     double latitude, double longitude, double altitude, 
                     double speed, String origin, String destination) {
        this.flightId = flightId;
        this.airline = airline;
        this.aircraft = aircraft;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.origin = origin;
        this.destination = destination;
        this.lastUpdate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }
    
    public String getAirline() { return airline; }
    public void setAirline(String airline) { this.airline = airline; }
    
    public String getAircraft() { return aircraft; }
    public void setAircraft(String aircraft) { this.aircraft = aircraft; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public double getAltitude() { return altitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }
    
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(LocalDateTime lastUpdate) { this.lastUpdate = lastUpdate; }
    
    @Override
    public String toString() {
        return String.format(
            "FlightData{flightId='%s', airline='%s', aircraft='%s', " +
            "lat=%.4f, lon=%.4f, alt=%.0f, speed=%.0f, origin='%s', destination='%s', update=%s}",
            flightId, airline, aircraft, latitude, longitude, altitude, speed, 
            origin, destination, lastUpdate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}