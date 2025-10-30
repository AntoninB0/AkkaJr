package com.example.akkajr.core;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * Hyperviseur qui supervise et récupère automatiquement les services
 */
public class Hypervisor {
    
    private static final Logger LOGGER = Logger.getLogger(Hypervisor.class.getName());
    
    private final Map<String, Service> services;
    private final ScheduledExecutorService healthCheckScheduler;
    private final ExecutorService recoveryExecutor;
    private final int healthCheckIntervalSeconds;
    private volatile boolean running;
    
    public Hypervisor() {
        this(10);  // Vérification toutes les 10 secondes par défaut
    }
    
    public Hypervisor(int healthCheckIntervalSeconds) {
        this.services = new ConcurrentHashMap<>();
        this.healthCheckScheduler = Executors.newScheduledThreadPool(1);
        this.recoveryExecutor = Executors.newCachedThreadPool();
        this.healthCheckIntervalSeconds = healthCheckIntervalSeconds;
        this.running = false;
    }
    
    /**
     * Enregistre un service à superviser
     */
    public void registerService(Service service) {
        services.put(service.getId(), service);
        LOGGER.info("Service enregistré : " + service.getName() + " [" + service.getId() + "]");
    }
    
    /**
     * Retire un service de la supervision
     */
    public void unregisterService(String serviceId) {
        Service removed = services.remove(serviceId);
        if (removed != null) {
            LOGGER.info("Service désenregistré : " + removed.getName());
        }
    }
    
    /**
     * Démarre l'hyperviseur
     */
    public void start() {
        if (running) {
            LOGGER.warning("L'hyperviseur est déjà en cours d'exécution");
            return;
        }
        
        running = true;
        LOGGER.info("🚀 Démarrage de l'hyperviseur");
        LOGGER.info("Intervalle de vérification : " + healthCheckIntervalSeconds + "s");
        
        // Planifie les health checks
        healthCheckScheduler.scheduleAtFixedRate(
            this::performHealthChecks,
            0,
            healthCheckIntervalSeconds,
            TimeUnit.SECONDS
        );
    }
    
    /**
     * Arrête l'hyperviseur
     */
    public void stop() {
        running = false;
        LOGGER.info("Arrêt de l'hyperviseur");
        
        healthCheckScheduler.shutdown();
        recoveryExecutor.shutdown();
        
        try {
            healthCheckScheduler.awaitTermination(10, TimeUnit.SECONDS);
            recoveryExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.warning("Timeout lors de l'arrêt");
            healthCheckScheduler.shutdownNow();
            recoveryExecutor.shutdownNow();
        }
    }
    
    /**
     * Effectue les vérifications de santé sur tous les services
     */
    private void performHealthChecks() {
        LOGGER.fine("Health check en cours...");
        
        for (Service service : services.values()) {
            try {
                checkServiceHealth(service);
            } catch (Exception e) {
                LOGGER.severe("Erreur lors du health check de " + service.getName() + 
                            ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Vérifie la santé d'un service spécifique
     */
    private void checkServiceHealth(Service service) {
        String serviceName = service.getName();
        String serviceId = service.getId();
        
        // Vérifie si le service est vivant
        if (!service.isAlive()) {
            LOGGER.warning("💀 Service MORT détecté : " + serviceName + " [" + serviceId + "]");
            LOGGER.warning("Dernier heartbeat : " + service.getLastHeartbeat());
            
            // Lance la récupération
            recoveryExecutor.submit(() -> recoverService(service));
        } else {
            LOGGER.fine("Service OK : " + serviceName);
        }
    }
    
    /**
     * Récupère un service mort
     */
    private void recoverService(Service deadService) {
        String serviceName = deadService.getName();
        String oldId = deadService.getId();
        
        LOGGER.warning("RÉCUPÉRATION EN COURS pour : " + serviceName);
        
        try {
            // 1. Sauvegarde le snapshot
            ServiceSnapshot snapshot = deadService.getLastSnapshot();
            if (snapshot == null) {
                snapshot = deadService.createSnapshot();
            }
            
            LOGGER.info("Snapshot récupéré : " + snapshot.pendingCommands.size() + " commandes");
            
            // 2. Arrête le service mort (si possible)
            try {
                deadService.stop();
            } catch (Exception e) {
                LOGGER.warning("Impossible d'arrêter proprement le service mort");
            }
            
            // 3. Crée une nouvelle instance
            Service newService = createNewServiceInstance(deadService, snapshot);
            
            if (newService == null) {
                LOGGER.severe("Impossible de créer une nouvelle instance");
                return;
            }
            
            // 4. Restaure l'état
            newService.restoreFromSnapshot(snapshot);
            
            // 5. Démarre le nouveau service
            newService.start();
            
            // 6. Remplace dans le registre
            services.remove(oldId);
            services.put(newService.getId(), newService);
            
            LOGGER.info("   RÉCUPÉRATION RÉUSSIE : " + serviceName);
            LOGGER.info("   Ancien ID : " + oldId);
            LOGGER.info("   Nouvel ID : " + newService.getId());
            LOGGER.info("   Commandes restaurées : " + snapshot.pendingCommands.size());
            
            // 7. Ré-exécute les commandes en attente
            if (!snapshot.pendingCommands.isEmpty()) {
                LOGGER.info("🔄 Ré-exécution des commandes en attente...");
                newService.execute();
            }
            
        } catch (Exception e) {
            LOGGER.severe("❌ ÉCHEC DE RÉCUPÉRATION pour " + serviceName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Crée une nouvelle instance du même type de service
     */
    private Service createNewServiceInstance(Service oldService, ServiceSnapshot snapshot) {
        try {
            // Utilise la réflexion pour créer une nouvelle instance
            Class<?> serviceClass = oldService.getClass();
            
            // Essaie le constructeur avec String (nom)
            try {
                return (Service) serviceClass
                    .getConstructor(String.class)
                    .newInstance(snapshot.serviceName);
            } catch (NoSuchMethodException e) {
                // Essaie le constructeur sans paramètres
                return (Service) serviceClass
                    .getConstructor()
                    .newInstance();
            }
            
        } catch (Exception e) {
            LOGGER.severe("Impossible de créer une nouvelle instance : " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Obtient le statut de tous les services
     */
    public Map<String, Map<String, Object>> getServicesStatus() {
        Map<String, Map<String, Object>> status = new HashMap<>();
        
        for (Service service : services.values()) {
            Map<String, Object> serviceStatus = new HashMap<>();
            serviceStatus.put("name", service.getName());
            serviceStatus.put("state", service.getState());
            serviceStatus.put("alive", service.isAlive());
            serviceStatus.put("lastHeartbeat", service.getLastHeartbeat());
            serviceStatus.put("pendingCommands", service.getInputsCommands().size());
            
            status.put(service.getId(), serviceStatus);
        }
        
        return status;
    }
    
    /**
     * Affiche le statut de tous les services
     */
    public void printStatus() {
        System.out.println("\n========== HYPERVISOR STATUS ==========");
        System.out.println("Services supervisés : " + services.size());
        System.out.println();
        
        for (Service service : services.values()) {
            String status = service.isAlive() ? "ALIVE" : "DEAD";
            System.out.printf("%-30s | %s | %s | Heartbeat: %s\n",
                            service.getName(),
                            service.getState(),
                            status,
                            service.getLastHeartbeat());
        }
        
        System.out.println("=======================================\n");
    }
}