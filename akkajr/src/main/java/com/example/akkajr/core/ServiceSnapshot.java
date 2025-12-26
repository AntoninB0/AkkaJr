package com.example.akkajr.core;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Snapshot de l'état d'un service pour récupération
 */
public class ServiceSnapshot {
    public String serviceId;
    public String serviceName;
    public String serviceClass;
    public Service.ServiceState state;
    public List<String> pendingCommands;
    public Map<String, String> configuration;
    public LocalDateTime snapshotTime;
    
    @Override
    public String toString() {
        return String.format("Snapshot[service=%s, commands=%d, time=%s]",
                           serviceName, 
                           pendingCommands != null ? pendingCommands.size() : 0, 
                           snapshotTime);
    }
}