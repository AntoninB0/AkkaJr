package com.example.akkajr.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.akkajr.core.WorkerHealthChecker;
import com.example.akkajr.core.WorkerRegistry;

/**
 * API pour gerer les workers dynamiquement
 */
@RestController
@RequestMapping("/api/workers")
public class WorkerController {
    
    private final WorkerRegistry workerRegistry;
    private final WorkerHealthChecker healthChecker;
    
    public WorkerController(WorkerRegistry workerRegistry, WorkerHealthChecker healthChecker) {
        this.workerRegistry = workerRegistry;
        this.healthChecker = healthChecker;
    }
    
    /**
     * Enregistrer un nouveau worker
     * POST /api/workers/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerWorker(@RequestBody WorkerRegistrationRequest request) {
        workerRegistry.register(
            request.getWorkerId(),
            request.getAddress(),
            request.getMetadata()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "registered");
        response.put("workerId", request.getWorkerId());
        response.put("message", "Worker enregistre avec succes");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Desenregistrer un worker
     */
    @DeleteMapping("/{workerId}")
    public ResponseEntity<?> unregisterWorker(@PathVariable String workerId) {
        workerRegistry.unregister(workerId);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "unregistered");
        response.put("workerId", workerId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Envoyer un heartbeat
     */
    @PostMapping("/{workerId}/heartbeat")
    public ResponseEntity<?> heartbeat(@PathVariable String workerId) {
        healthChecker.heartbeat(workerId);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("workerId", workerId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lister tous les workers
     */
    @GetMapping
    public ResponseEntity<List<WorkerRegistry.WorkerInfo>> listWorkers() {
        return ResponseEntity.ok(workerRegistry.getAllWorkers());
    }
    
    /**
     * Lister les workers disponibles
     */
    @GetMapping("/available")
    public ResponseEntity<List<WorkerRegistry.WorkerInfo>> listAvailableWorkers() {
        return ResponseEntity.ok(workerRegistry.getAvailableWorkers());
    }
    
    /**
     * Recuperer un worker specifique
     */
    @GetMapping("/{workerId}")
    public ResponseEntity<?> getWorker(@PathVariable String workerId) {
        return workerRegistry.getWorker(workerId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Mettre a jour le statut d'un worker
     */
    @PutMapping("/{workerId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String workerId,
            @RequestBody StatusUpdateRequest request) {
        
        workerRegistry.updateStatus(workerId, request.getStatus());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "updated");
        response.put("workerId", workerId);
        response.put("newStatus", request.getStatus());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Rechercher des workers par tag
     */
    @GetMapping("/search")
    public ResponseEntity<List<WorkerRegistry.WorkerInfo>> searchWorkers(
            @RequestParam String tag,
            @RequestParam String value) {
        
        return ResponseEntity.ok(workerRegistry.getWorkersByTag(tag, value));
    }
    
    // ==================== DTOs ====================
    
    public static class WorkerRegistrationRequest {
        private String workerId;
        private String address;
        private Map<String, String> metadata;
        
        public String getWorkerId() { return workerId; }
        public void setWorkerId(String workerId) { this.workerId = workerId; }
        
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    }
    
    public static class StatusUpdateRequest {
        private WorkerRegistry.WorkerStatus status;
        
        public WorkerRegistry.WorkerStatus getStatus() { return status; }
        public void setStatus(WorkerRegistry.WorkerStatus status) { this.status = status; }
    }
}