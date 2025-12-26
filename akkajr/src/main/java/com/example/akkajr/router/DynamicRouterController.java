package com.example.akkajr.router;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API pour la selection dynamique de workers
 * 
 * Ce controller selectionne uniquement le meilleur worker disponible.
 * Le client doit ensuite envoyer le message au worker selectionne.
 * 
 * Endpoints : /api/router/dynamic/*
 */
@RestController
@RequestMapping("/api/router/dynamic")
public class DynamicRouterController {
    
    private final DynamicRoundRobinRouter dynamicRoundRobinRouter;
    private final LoadBalancedRouter loadBalancedRouter;
    
    public DynamicRouterController(
            DynamicRoundRobinRouter dynamicRoundRobinRouter,
            LoadBalancedRouter loadBalancedRouter) {
        this.dynamicRoundRobinRouter = dynamicRoundRobinRouter;
        this.loadBalancedRouter = loadBalancedRouter;
    }
    
    /**
     * Selection Round-Robin DYNAMIQUE
     * 
     * POST /api/router/dynamic/roundrobin
     * 
     * Selectionne le prochain worker disponible en round-robin.
     * Retourne les informations du worker selectionne (id + adresse).
     * 
     * Body: {
     *   "filter": {
     *     "tag": "region",
     *     "value": "eu-west"
     *   }
     * }
     * 
     * Response: {
     *   "success": true,
     *   "workerId": "worker-1",
     *   "workerAddress": "http://localhost:9001",
     *   "message": "Worker selectionne avec succes",
     *   "strategy": "ROUND_ROBIN"
     * }
     */
    @PostMapping("/roundrobin")
    public ResponseEntity<?> selectRoundRobin(@RequestBody(required = false) WorkerSelectionRequest request) {
        DynamicRoundRobinRouter.WorkerFilter filter = null;
        
        if (request != null && request.getFilter() != null) {
            filter = new DynamicRoundRobinRouter.WorkerFilter(
                request.getFilter().get("tag"),
                request.getFilter().get("value")
            );
        }
        
        DynamicRoundRobinRouter.RouteResult result = dynamicRoundRobinRouter.selectWorker(filter);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("workerId", result.getWorkerId());
        response.put("workerAddress", result.getWorkerAddress());
        response.put("message", result.getMessage());
        response.put("strategy", "ROUND_ROBIN");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Selection Load Balanced DYNAMIQUE
     * 
     * POST /api/router/dynamic/loadbalanced
     * 
     * Selectionne le worker avec la charge la plus faible.
     * Retourne les informations du worker selectionne (id + adresse + charge).
     * 
     * Body: {
     *   "filter": {
     *     "tag": "capability",
     *     "value": "payment"
     *   }
     * }
     * 
     * Response: {
     *   "success": true,
     *   "workerId": "worker-2",
     *   "workerAddress": "http://localhost:9002",
     *   "workerLoad": 0,
     *   "message": "Worker selectionne avec succes",
     *   "strategy": "LOAD_BALANCED"
     * }
     */
    @PostMapping("/loadbalanced")
    public ResponseEntity<?> selectLoadBalanced(@RequestBody(required = false) WorkerSelectionRequest request) {
        LoadBalancedRouter.WorkerFilter filter = null;
        
        if (request != null && request.getFilter() != null) {
            filter = new LoadBalancedRouter.WorkerFilter(
                request.getFilter().get("tag"),
                request.getFilter().get("value")
            );
        }
        
        LoadBalancedRouter.RouteResult result = loadBalancedRouter.selectWorker(filter);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("workerId", result.getWorkerId());
        response.put("workerAddress", result.getWorkerAddress());
        response.put("workerLoad", result.getWorkerLoad());
        response.put("message", result.getMessage());
        response.put("strategy", "LOAD_BALANCED");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Demonstration des strategies de selection
     * 
     * POST /api/router/dynamic/demo
     * 
     * Teste les deux strategies et retourne les resultats.
     */
    @PostMapping("/demo")
    public ResponseEntity<?> demoStrategies() {
        Map<String, Object> response = new HashMap<>();
        
        // 1. Round-Robin
        DynamicRoundRobinRouter.RouteResult rrResult = dynamicRoundRobinRouter.selectWorker();
        response.put("roundRobin", Map.of(
            "success", rrResult.isSuccess(),
            "workerId", rrResult.getWorkerId() != null ? rrResult.getWorkerId() : "none",
            "workerAddress", rrResult.getWorkerAddress() != null ? rrResult.getWorkerAddress() : "none"
        ));
        
        // 2. Load Balanced
        LoadBalancedRouter.RouteResult lbResult = loadBalancedRouter.selectWorker();
        response.put("loadBalanced", Map.of(
            "success", lbResult.isSuccess(),
            "workerId", lbResult.getWorkerId() != null ? lbResult.getWorkerId() : "none",
            "workerAddress", lbResult.getWorkerAddress() != null ? lbResult.getWorkerAddress() : "none",
            "load", lbResult.getWorkerLoad()
        ));
        
        response.put("message", "Demonstration des strategies de selection");
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== DTOs ====================
    
    /**
     * Requete de selection de worker
     */
    public static class WorkerSelectionRequest {
        private Map<String, String> filter;
        
        public Map<String, String> getFilter() { return filter; }
        public void setFilter(Map<String, String> filter) { this.filter = filter; }
    }
}