package com.example.akkajr.router;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.akkajr.core.WorkerRegistry;

/**
 * Router intelligent avec load balancing
 * Selectionne le worker avec la charge la plus faible
 * 
 * NOTE: Ce router ne fait que SELECTIONNER le worker.
 * L'envoi du message doit etre fait par le client ou un autre composant.
 */
@Component
public class LoadBalancedRouter {
    
    private static final Logger log = LoggerFactory.getLogger(LoadBalancedRouter.class);
    
    private final WorkerRegistry workerRegistry;
    
    public LoadBalancedRouter(WorkerRegistry workerRegistry) {
        this.workerRegistry = workerRegistry;
    }
    
    /**
     * Selectionne le worker avec la charge la plus faible
     * 
     * @return Informations du worker selectionne
     */
    public RouteResult selectWorker() {
        return selectWorker(null);
    }
    
    /**
     * Selectionne le worker avec la charge la plus faible (avec filtre)
     * 
     * @param filter Filtre optionnel par tag
     * @return Informations du worker selectionne
     */
    public RouteResult selectWorker(WorkerFilter filter) {
        Optional<WorkerRegistry.WorkerInfo> workerOpt = findLeastLoadedWorker(filter);
        
        if (workerOpt.isEmpty()) {
            log.warn("[LoadBalancedRouter] Aucun worker disponible");
            return new RouteResult(false, null, null, 0, "Aucun worker disponible");
        }
        
        WorkerRegistry.WorkerInfo selectedWorker = workerOpt.get();
        int previousLoad = selectedWorker.getCurrentLoad();
        
        // Incrementer la charge
        workerRegistry.incrementLoad(selectedWorker.getId());
        
        log.info("[LoadBalancedRouter] Worker selectionne: {} (charge: {} -> {})", 
            selectedWorker.getId(), previousLoad, previousLoad + 1);
        
        // Decrementer la charge apres traitement (simulation)
        decrementLoadAsync(selectedWorker.getId(), 1000);
        
        return new RouteResult(
            true, 
            selectedWorker.getId(), 
            selectedWorker.getAddress(),
            previousLoad, 
            "Worker selectionne avec succes"
        );
    }
    
    private Optional<WorkerRegistry.WorkerInfo> findLeastLoadedWorker(WorkerFilter filter) {
        List<WorkerRegistry.WorkerInfo> workers = getFilteredWorkers(filter);
        
        if (workers.isEmpty()) {
            return Optional.empty();
        }
        
        return workers.stream()
            .min((w1, w2) -> Integer.compare(w1.getCurrentLoad(), w2.getCurrentLoad()));
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
                log.debug("[LoadBalancedRouter] Charge decrementee pour {}", workerId);
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
        private final int workerLoad;
        private final String message;
        
        public RouteResult(boolean success, String workerId, String workerAddress, 
                          int workerLoad, String message) {
            this.success = success;
            this.workerId = workerId;
            this.workerAddress = workerAddress;
            this.workerLoad = workerLoad;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getWorkerId() { return workerId; }
        public String getWorkerAddress() { return workerAddress; }
        public int getWorkerLoad() { return workerLoad; }
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