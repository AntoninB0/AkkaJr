package com.example.akkajr.core;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * Hyperviseur qui supervise et récupère automatiquement les services
 * avec stratégies de supervision inspirées d'Akka
 */
public class Hypervisor implements Supervisor {
    
    private static final Logger LOGGER = Logger.getLogger(Hypervisor.class.getName());
    
    private final Map<String, Service> services;
    private final ScheduledExecutorService healthCheckScheduler;
    private final ExecutorService recoveryExecutor;
    private final int healthCheckIntervalSeconds;
    private volatile boolean running;
    
    // Stratégies de supervision
    private Supervisor.SupervisionMode supervisionMode;
    private Map<String, String> serviceDecisions;
    
    public Hypervisor() {
        this(10);  // Vérification toutes les 10 secondes par défaut
    }
    
    public Hypervisor(int healthCheckIntervalSeconds) {
        this.services = new ConcurrentHashMap<>();
        this.healthCheckScheduler = Executors.newScheduledThreadPool(1);
        this.recoveryExecutor = Executors.newCachedThreadPool();
        this.healthCheckIntervalSeconds = healthCheckIntervalSeconds;
        this.running = false;
        
        // Configuration supervision
        this.supervisionMode = Supervisor.SupervisionMode.ONE_FOR_ONE;
        this.serviceDecisions = new ConcurrentHashMap<>();
    }
    
    // ========== IMPLÉMENTATION SUPERVISOR ==========
    
    @Override
    public void handleFailure(Service failedService, Throwable cause) {
        String serviceName = failedService.getName();
        String serviceId = failedService.getId();
        
        LOGGER.severe("⚠️ ÉCHEC DÉTECTÉ: " + serviceName + " [" + serviceId + "]");
        if (cause != null) {
            LOGGER.severe("Cause: " + cause.getMessage());
        }
        
        LOGGER.info("Mode de supervision: " + supervisionMode);
        
        // Applique la stratégie selon le mode
        if (supervisionMode == Supervisor.SupervisionMode.ONE_FOR_ONE) {
            LOGGER.info("📋 Application ONE_FOR_ONE pour " + serviceName);
            recoverService(failedService);
        } else {
            LOGGER.warning("📋 Application ALL_FOR_ONE pour TOUS les services");
            LOGGER.warning("   Cause: échec de " + serviceName);
            
            // Récupère tous les services
            for (Service service : services.values()) {
                try {
                    recoverService(service);
                } catch (Exception e) {
                    LOGGER.severe("Erreur lors de la récupération de " + 
                                service.getName() + ": " + e.getMessage());
                }
            }
        }
    }
    
    @Override
    public Supervisor.SupervisionMode getSupervisionMode() {
        return supervisionMode;
    }
    
    @Override
    public void setSupervisionMode(Supervisor.SupervisionMode mode) {
        this.supervisionMode = mode;
        LOGGER.info("Mode de supervision changé: " + mode);
    }
    
    // ========== GESTION DES SERVICES ==========
    
    /**
     * Enregistre un service à superviser
     */
    public void registerService(Service service) {
        services.put(service.getId(), service);
        LOGGER.info("✅ Service enregistré: " + service.getName() + " [" + service.getId() + "]");
    }
    
    /**
     * Retire un service de la supervision
     */
    public void unregisterService(String serviceId) {
        Service removed = services.remove(serviceId);
        serviceDecisions.remove(serviceId);
        if (removed != null) {
            LOGGER.info("❌ Service désenregistré: " + removed.getName());
        }
    }
    
    // ========== CYCLE DE VIE ==========
    
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
        LOGGER.info("   Mode: " + supervisionMode);
        LOGGER.info("   Intervalle de vérification: " + healthCheckIntervalSeconds + "s");
        
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
        LOGGER.info("🛑 Arrêt de l'hyperviseur");
        
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
    
    // ========== HEALTH CHECKS ==========
    
    /**
     * Effectue les vérifications de santé sur tous les services
     */
    private void performHealthChecks() {
        LOGGER.fine("💓 Health check en cours...");
        
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
        if (!service.isAlive() && service.getState() != Service.ServiceState.STOPPED) {
            LOGGER.warning("💀 Service MORT détecté: " + serviceName + " [" + serviceId + "]");
            LOGGER.warning("   Dernier heartbeat: " + service.getLastHeartbeat());
            LOGGER.warning("   État: " + service.getState());
            
            // Lance la gestion de l'échec
            recoveryExecutor.submit(() -> handleFailure(service, null));
        } else {
            LOGGER.fine("✓ Service OK: " + serviceName);
        }
    }
    
    // ========== RÉCUPÉRATION ==========
    
    /**
     * Récupère un service mort
     */
    private void recoverService(Service deadService) {
        String serviceName = deadService.getName();
        String oldId = deadService.getId();
        
        LOGGER.warning("🔄 RÉCUPÉRATION EN COURS pour: " + serviceName);
        
        try {
            // 1. Sauvegarde le snapshot
            ServiceSnapshot snapshot = deadService.getLastSnapshot();
            if (snapshot == null) {
                snapshot = deadService.createSnapshot();
            }
            
            LOGGER.info("   Snapshot récupéré: " + snapshot.pendingCommands.size() + " commandes");
            
            // 2. Arrête le service mort (si possible)
            try {
                deadService.stop();
                Thread.sleep(1000); // Pause avant redémarrage
            } catch (Exception e) {
                LOGGER.warning("   Impossible d'arrêter proprement le service mort");
            }
            
            // 3. Crée une nouvelle instance
            Service newService = createNewServiceInstance(deadService, snapshot);
            
            if (newService == null) {
                LOGGER.severe("   ❌ Impossible de créer une nouvelle instance");
                return;
            }
            
            // 4. Restaure l'état
            newService.restoreFromSnapshot(snapshot);
            
            // 5. Démarre le nouveau service
            newService.start();
            
            // 6. Remplace dans le registre
            services.remove(oldId);
            services.put(newService.getId(), newService);
            
            LOGGER.info("   ✅ RÉCUPÉRATION RÉUSSIE: " + serviceName);
            LOGGER.info("   Ancien ID: " + oldId);
            LOGGER.info("   Nouvel ID: " + newService.getId());
            LOGGER.info("   Commandes restaurées: " + snapshot.pendingCommands.size());
            
            // 7. Ré-exécute les commandes en attente
            if (!snapshot.pendingCommands.isEmpty()) {
                LOGGER.info("   🔄 Ré-exécution des commandes en attente...");
                newService.execute();
            }
            
        } catch (Exception e) {
            LOGGER.severe("   ❌ ÉCHEC DE RÉCUPÉRATION pour " + serviceName + ": " + e.getMessage());
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
            LOGGER.severe("Impossible de créer une nouvelle instance: " + e.getMessage());
            return null;
        }
    }
    
    // ========== STATUS ET MONITORING ==========
    
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
            serviceStatus.put("childrenCount", service.getChildrenCount());
            
            status.put(service.getId(), serviceStatus);
        }
        
        return status;
    }
    
    /**
     * Affiche le statut de tous les services
     */
    public void printStatus() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("                      HYPERVISOR STATUS");
        System.out.println("=".repeat(80));
        System.out.println("Mode de supervision    : " + supervisionMode);
        System.out.println("Services supervisés   : " + services.size());
        System.out.println("État                  : " + (running ? "RUNNING" : "STOPPED"));
        System.out.println("-".repeat(80));
        
        if (services.isEmpty()) {
            System.out.println("Aucun service enregistré");
        } else {
            System.out.printf("%-25s | %-10s | %-6s | %-8s%n",
                            "SERVICE", "STATE", "ALIVE", "CHILDREN");
            System.out.println("-".repeat(80));
            
            for (Service service : services.values()) {
                String status = service.isAlive() ? "✓" : "✗";
                
                System.out.printf("%-25s | %-10s | %-6s | %-8d%n",
                                truncate(service.getName(), 25),
                                service.getState(),
                                status,
                                service.getChildrenCount());
            }
        }
        
        System.out.println("=".repeat(80) + "\n");
    }
    
    private String truncate(String str, int length) {
        return str.length() > length ? str.substring(0, length - 3) + "..." : str;
    }
    
    /**
     * Retourne tous les services enregistrés
     */
    public Collection<Service> getServices() {
        return new ArrayList<>(services.values());
    }
    
    /**
     * Retourne un service par son ID
     */
    public Service getService(String serviceId) {
        return services.get(serviceId);
    }
    
    public boolean isRunning() {
        return running;
    }
}