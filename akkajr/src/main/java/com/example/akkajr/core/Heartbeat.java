package com.example.akkajr.core;

import java.time.LocalDateTime;

/**
 * Interface pour le système de heartbeat
 */
public interface Heartbeat {
    
    /**
     * Envoie un signal de vie
     */
    void ping();
    
    /**
     * Récupère le dernier heartbeat
     */
    LocalDateTime getLastHeartbeat();
    
    /**
     * Vérifie si le service est vivant
     */
    boolean isAlive();
    
    /**
     * Définit le timeout du heartbeat (en secondes)
     */
    void setHeartbeatTimeout(int seconds);
}