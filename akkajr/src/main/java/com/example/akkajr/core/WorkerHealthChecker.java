package com.example.akkajr.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Service de surveillance de la sante des workers
 */
@Service
public class WorkerHealthChecker {
    
    private static final Logger log = LoggerFactory.getLogger(WorkerHealthChecker.class);
    
    private final WorkerRegistry workerRegistry;
    
    // Timeout du heartbeat (30 secondes)
    private static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(30);
    
    public WorkerHealthChecker(WorkerRegistry workerRegistry) {
        this.workerRegistry = workerRegistry;
    }
    
    /**
     * Verifier periodiquement la sante des workers
     * Execute toutes les 10 secondes
     */
    @Scheduled(fixedRate = 10000)
    public void checkWorkerHealth() {
        List<WorkerRegistry.WorkerInfo> workers = workerRegistry.getAllWorkers();
        
        for (WorkerRegistry.WorkerInfo worker : workers) {
            checkWorker(worker);
        }
    }
    
    /**
     * Verifier un worker specifique
     */
    private void checkWorker(WorkerRegistry.WorkerInfo worker) {
        Instant now = Instant.now();
        Duration timeSinceHeartbeat = Duration.between(worker.getLastHeartbeat(), now);
        
        if (timeSinceHeartbeat.compareTo(HEARTBEAT_TIMEOUT) > 0) {
            // Heartbeat manque - marquer comme OFFLINE
            if (worker.getStatus() != WorkerRegistry.WorkerStatus.OFFLINE) {
                log.warn("[HealthChecker] Worker {} est OFFLINE (pas de heartbeat depuis {}s)", 
                    worker.getId(), timeSinceHeartbeat.getSeconds());
                workerRegistry.updateStatus(worker.getId(), WorkerRegistry.WorkerStatus.OFFLINE);
            }
        } else {
            // Worker est actif
            if (worker.getStatus() == WorkerRegistry.WorkerStatus.OFFLINE) {
                log.info("[HealthChecker] Worker {} est de nouveau AVAILABLE", worker.getId());
                workerRegistry.updateStatus(worker.getId(), WorkerRegistry.WorkerStatus.AVAILABLE);
            }
        }
    }
    
    /**
     * Enregistrer un heartbeat pour un worker
     */
    public void heartbeat(String workerId) {
        workerRegistry.getWorker(workerId).ifPresent(worker -> {
            worker.updateHeartbeat();
            
            // Si le worker etait OFFLINE, le remettre AVAILABLE
            if (worker.getStatus() == WorkerRegistry.WorkerStatus.OFFLINE) {
                workerRegistry.updateStatus(workerId, WorkerRegistry.WorkerStatus.AVAILABLE);
                log.info("[HealthChecker] Worker {} reconnecte", workerId);
            }
        });
    }
}