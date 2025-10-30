package com.example.akkajr.controllers;

import com.example.akkajr.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/services")
public class TestController {
    
    @Autowired
    private Hypervisor hypervisor;
    
    // Map pour stocker les services créés
    private Map<String, Service> servicesMap = new HashMap<>();
    
    /**
     * GET /api/services - Liste tous les services
     */
    @GetMapping
    public ResponseEntity<Map<String, Map<String, Object>>> getAllServices() {
        return ResponseEntity.ok(hypervisor.getServicesStatus());
    }
    
    /**
     * POST /api/services - Crée un nouveau service
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createService(@RequestBody CreateServiceRequest request) {
        try {
            // Crée un service de test
            TestService service = new TestService(request.getName());
            
            // Configure
            if (request.getHeartbeatTimeout() != null) {
                service.setHeartbeatTimeout(request.getHeartbeatTimeout());
            }
            
            if (request.getCrashMode() != null) {
                service.setCrashMode(request.getCrashMode());
            }
            
            // Démarre
            service.start();
            
            // Enregistre
            hypervisor.registerService(service);
            servicesMap.put(service.getId(), service);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", service.getId());
            response.put("name", service.getName());
            response.put("state", service.getState());
            response.put("message", "Service créé et enregistré");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * POST /api/services/{id}/commands - Ajoute une commande
     */
    @PostMapping("/{id}/commands")
    public ResponseEntity<Map<String, Object>> addCommand(
            @PathVariable String id,
            @RequestBody AddCommandRequest request) {
        
        Service service = servicesMap.get(id);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        
        service.addCommand(request.getCommand());
        
        return ResponseEntity.ok(Map.of(
            "message", "Commande ajoutée",
            "command", request.getCommand(),
            "totalCommands", service.getInputsCommands().size()
        ));
    }
    
    /**
     * POST /api/services/{id}/execute - Exécute les commandes
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeCommands(@PathVariable String id) {
        Service service = servicesMap.get(id);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Exécute dans un thread séparé
        new Thread(() -> {
            try {
                service.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        
        return ResponseEntity.ok(Map.of(
            "message", "Exécution lancée",
            "commands", service.getInputsCommands().size()
        ));
    }
    
    /**
     * GET /api/services/{id} - Détails d'un service
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getService(@PathVariable String id) {
        Service service = servicesMap.get(id);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> details = new HashMap<>();
        details.put("id", service.getId());
        details.put("name", service.getName());
        details.put("state", service.getState());
        details.put("alive", service.isAlive());
        details.put("lastHeartbeat", service.getLastHeartbeat());
        details.put("pendingCommands", service.getInputsCommands());
        
        return ResponseEntity.ok(details);
    }
    
    /**
     * DELETE /api/services/{id} - Arrête et supprime un service
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteService(@PathVariable String id) {
        Service service = servicesMap.get(id);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            service.stop();
            hypervisor.unregisterService(id);
            servicesMap.remove(id);
            
            return ResponseEntity.ok(Map.of("message", "Service supprimé"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * GET /api/hypervisor/status - Statut de l'hyperviseur
     */
    @GetMapping("/hypervisor/status")
    public ResponseEntity<Map<String, Object>> getHypervisorStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("services", hypervisor.getServicesStatus());
        status.put("totalServices", hypervisor.getServicesStatus().size());
        
        return ResponseEntity.ok(status);
    }
}

// ========== DTOs ==========

class CreateServiceRequest {
    private String name;
    private Integer heartbeatTimeout;
    private Boolean crashMode;
    
    // Getters et Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Integer getHeartbeatTimeout() { return heartbeatTimeout; }
    public void setHeartbeatTimeout(Integer heartbeatTimeout) { 
        this.heartbeatTimeout = heartbeatTimeout; 
    }
    
    public Boolean getCrashMode() { return crashMode; }
    public void setCrashMode(Boolean crashMode) { this.crashMode = crashMode; }
}

class AddCommandRequest {
    private String command;
    
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
}