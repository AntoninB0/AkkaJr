package com.example.akkajr.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registre dynamique des workers pour le routage intelligent
 */
@Component
public class WorkerRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(WorkerRegistry.class);
    
    // Stockage des workers enregistres
    private final Map<String, WorkerInfo> workers = new ConcurrentHashMap<>();
    
    // Listeners pour les changements
    private final List<WorkerRegistryListener> listeners = new CopyOnWriteArrayList<>();
    
    /**
     * Enregistrer un nouveau worker
     */
    public void register(String workerId, String address, Map<String, String> metadata) {
        WorkerInfo worker = new WorkerInfo(
            workerId,
            address,
            WorkerStatus.AVAILABLE,
            metadata,
            Instant.now(),
            0
        );
        
        workers.put(workerId, worker);
        log.info("[WorkerRegistry] Worker enregistre: {} @ {}", workerId, address);
        
        // Notifier les listeners
        notifyListeners(new WorkerEvent(WorkerEventType.REGISTERED, worker));
    }
    
    /**
     * Desenregistrer un worker
     */
    public void unregister(String workerId) {
        WorkerInfo worker = workers.remove(workerId);
        if (worker != null) {
            log.info("[WorkerRegistry] Worker desenregistre: {}", workerId);
            notifyListeners(new WorkerEvent(WorkerEventType.UNREGISTERED, worker));
        }
    }
    
    /**
     * Recuperer tous les workers disponibles
     */
    public List<WorkerInfo> getAvailableWorkers() {
        return workers.values().stream()
            .filter(w -> w.status == WorkerStatus.AVAILABLE)
            .collect(Collectors.toList());
    }
    
    /**
     * Recuperer tous les workers (tous statuts)
     */
    public List<WorkerInfo> getAllWorkers() {
        return new ArrayList<>(workers.values());
    }
    
    /**
     * Recuperer un worker specifique
     */
    public Optional<WorkerInfo> getWorker(String workerId) {
        return Optional.ofNullable(workers.get(workerId));
    }
    
    /**
     * Mettre a jour le statut d'un worker
     */
    public void updateStatus(String workerId, WorkerStatus status) {
        WorkerInfo worker = workers.get(workerId);
        if (worker != null) {
            worker.status = status;
            log.info("[WorkerRegistry] Worker {} statut mis a jour: {}", workerId, status);
            notifyListeners(new WorkerEvent(WorkerEventType.STATUS_CHANGED, worker));
        }
    }
    
    /**
     * Incrementer la charge d'un worker
     */
    public void incrementLoad(String workerId) {
        WorkerInfo worker = workers.get(workerId);
        if (worker != null) {
            worker.incrementLoad();
        }
    }
    
    /**
     * Decrementer la charge d'un worker
     */
    public void decrementLoad(String workerId) {
        WorkerInfo worker = workers.get(workerId);
        if (worker != null) {
            worker.decrementLoad();
        }
    }
    
    /**
     * Recuperer le worker avec la charge la plus faible
     */
    public Optional<WorkerInfo> getLeastLoadedWorker() {
        return getAvailableWorkers().stream()
            .min(Comparator.comparingInt(w -> w.currentLoad));
    }
    
    /**
     * Recuperer les workers par tag
     */
    public List<WorkerInfo> getWorkersByTag(String tag, String value) {
        return getAvailableWorkers().stream()
            .filter(w -> value.equals(w.metadata.get(tag)))
            .collect(Collectors.toList());
    }
    
    /**
     * Ajouter un listener
     */
    public void addListener(WorkerRegistryListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Notifier les listeners
     */
    private void notifyListeners(WorkerEvent event) {
        for (WorkerRegistryListener listener : listeners) {
            try {
                listener.onWorkerEvent(event);
            } catch (Exception e) {
                log.error("[WorkerRegistry] Erreur dans le listener", e);
            }
        }
    }
    
    /**
     * Informations sur un worker
     */
    public static class WorkerInfo {
        private final String id;
        private final String address;
        private WorkerStatus status;
        private final Map<String, String> metadata;
        private final Instant registeredAt;
        private Instant lastHeartbeat;
        private int currentLoad;
        
        public WorkerInfo(String id, String address, WorkerStatus status, 
                         Map<String, String> metadata, Instant registeredAt, int currentLoad) {
            this.id = id;
            this.address = address;
            this.status = status;
            this.metadata = metadata != null ? metadata : new HashMap<>();
            this.registeredAt = registeredAt;
            this.lastHeartbeat = Instant.now();
            this.currentLoad = currentLoad;
        }
        
        // Getters
        public String getId() { return id; }
        public String getAddress() { return address; }
        public WorkerStatus getStatus() { return status; }
        public Map<String, String> getMetadata() { return metadata; }
        public Instant getRegisteredAt() { return registeredAt; }
        public Instant getLastHeartbeat() { return lastHeartbeat; }
        public int getCurrentLoad() { return currentLoad; }
        
        public void updateHeartbeat() {
            this.lastHeartbeat = Instant.now();
        }
        
        public synchronized void incrementLoad() {
            this.currentLoad++;
        }
        
        public synchronized void decrementLoad() {
            this.currentLoad = Math.max(0, this.currentLoad - 1);
        }
    }
    
    /**
     * Statut d'un worker
     */
    public enum WorkerStatus {
        AVAILABLE,    // Pret a recevoir des messages
        BUSY,         // En train de traiter
        UNAVAILABLE,  // Temporairement indisponible
        OFFLINE       // Hors ligne
    }
    
    /**
     * Evenement du registre
     */
    public static class WorkerEvent {
        private final WorkerEventType type;
        private final WorkerInfo worker;
        private final Instant timestamp;
        
        public WorkerEvent(WorkerEventType type, WorkerInfo worker) {
            this.type = type;
            this.worker = worker;
            this.timestamp = Instant.now();
        }
        
        public WorkerEventType getType() { return type; }
        public WorkerInfo getWorker() { return worker; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    /**
     * Type d'evenement
     */
    public enum WorkerEventType {
        REGISTERED,
        UNREGISTERED,
        STATUS_CHANGED,
        HEARTBEAT_MISSED
    }
    
    /**
     * Interface pour les listeners
     */
    public interface WorkerRegistryListener {
        void onWorkerEvent(WorkerEvent event);
    }
}