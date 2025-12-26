package com.example.akkajr.router;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.akkajr.core.WorkerRegistry;

/**
 * Router Round-Robin DYNAMIQUE
 * Selectionne le prochain worker disponible (auto-decouverte)
 * 
 * NOTE: Ce router ne fait que SELECTIONNER le worker.
 * L'envoi du message doit etre fait par le client ou un autre composant.
 */
@Component
public class DynamicRoundRobinRouter {
    
    private static final Logger log = LoggerFactory.getLogger(DynamicRoundRobinRouter.class);
    
    private final WorkerRegistry workerRegistry;
    private final AtomicInteger counter = new AtomicInteger(0);
    
    public DynamicRoundRobinRouter(WorkerRegistry workerRegistry) {
        this.workerRegistry = workerRegistry;
    }
    
    /**
     * Selectionne le prochain worker disponible (round-robin)
     * 
     * @return Informations du worker selectionne ou null si aucun disponible
     */
    public RouteResult selectWorker() {
        return selectWorker(null);
    }
    
    /**
     * Selectionne le prochain worker avec filtre par tag
     * 
     * @param filter Filtre optionnel par tag
     * @return Informations du worker selectionne
     */
    public RouteResult selectWorker(WorkerFilter filter) {
        List<WorkerRegistry.WorkerInfo> availableWorkers = getFilteredWorkers(filter);
        
        if (availableWorkers.isEmpty()) {
            log.warn("[DynamicRoundRobinRouter] Aucun worker disponible");
            return new RouteResult(false, null, null, "Aucun worker disponible");
        }
        
        int index = counter.getAndIncrement() % availableWorkers.size();
        WorkerRegistry.WorkerInfo selectedWorker = availableWorkers.get(index);
        
        // Incrementer la charge du worker selectionne
        workerRegistry.incrementLoad(selectedWorker.getId());
        
        log.info("[DynamicRoundRobinRouter] Worker selectionne: {} (index {}/{})", 
            selectedWorker.getId(), index, availableWorkers.size());
        
        // Decrementer la charge apres un delai (simulation)
        decrementLoadAsync(selectedWorker.getId(), 1000);
        
        return new RouteResult(
            true, 
            selectedWorker.getId(), 
            selectedWorker.getAddress(),
            "Worker selectionne avec succes"
        );
    }
    
    private List<WorkerRegistry.WorkerInfo> getFilteredWorkers(WorkerFilter filter) {
        if (filter != null && filter.getTag() != null) {
            return workerRegistry.getWorkersByTag(filter.getTag(), filter.getValue());
        }
        return workerRegistry.getAvailableWorkers();
    }
    
    private void decrementLoadAsync(String workerId, long delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                workerRegistry.decrementLoad(workerId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Resultat de la selection
     */
    public static class RouteResult {
        private final boolean success;
        private final String workerId;
        private final String workerAddress;
        private final String message;
        
        public RouteResult(boolean success, String workerId, String workerAddress, String message) {
            this.success = success;
            this.workerId = workerId;
            this.workerAddress = workerAddress;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getWorkerId() { return workerId; }
        public String getWorkerAddress() { return workerAddress; }
        public String getMessage() { return message; }
    }
    
    /**
     * Filtre pour les workers
     */
    public static class WorkerFilter {
        private final String tag;
        private final String value;
        
        public WorkerFilter(String tag, String value) {
            this.tag = tag;
            this.value = value;
        }
        
        public String getTag() { return tag; }
        public String getValue() { return value; }
    }
}