package com.example.akkajr.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Hypervisor - Gestionnaire central des services
 * Surveille l'état des services et gère leur cycle de vie
 */
public class Hypervisor {
    
    private static final Logger logger = LoggerFactory.getLogger(Hypervisor.class);
    
    // Map des services enregistrés (serviceId -> Service)
    private final Map<String, Service> services;
    
    // Executor pour les tâches de monitoring
    private final ScheduledExecutorService scheduler;
    
    // Intervalle de vérification du heartbeat (en millisecondes)
    private final long healthCheckInterval;
    
    // Timeout pour le heartbeat (en millisecondes)
    private final long heartbeatTimeout;
    
    // État de l'hypervisor
    private volatile boolean running;
    
    /**
     * Constructeur
     * @param healthCheckInterval Intervalle de vérification (ms)
     */
    public Hypervisor(long healthCheckInterval) {
        this.services = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.healthCheckInterval = healthCheckInterval;
        this.heartbeatTimeout = healthCheckInterval * 3; // 3x l'intervalle
        this.running = false;
        
        logger.info("Hypervisor créé avec intervalle de {}ms", healthCheckInterval);
    }
    
    /**
     * Démarre l'hypervisor
     */
    public void start() {
        if (running) {
            logger.warn("Hypervisor déjà démarré");
            return;
        }
        
        running = true;
        logger.info("Démarrage de l'Hypervisor...");
        
        // Planifier la vérification périodique des heartbeats
        scheduler.scheduleAtFixedRate(
            this::checkHeartbeats,
            healthCheckInterval,
            healthCheckInterval,
            TimeUnit.MILLISECONDS
        );
        
        logger.info("Hypervisor démarré avec succès");
    }
    
    /**
     * Arrête l'hypervisor
     */
    public void stop() {
        if (!running) {
            logger.warn("Hypervisor déjà arrêté");
            return;
        }
        
        running = false;
        logger.info("Arrêt de l'Hypervisor...");
        
        // Arrêter tous les services
        services.values().forEach(service -> {
            try {
                service.stop();
            } catch (Exception e) {
                logger.error("Erreur lors de l'arrêt du service {}", service.getId(), e);
            }
        });
        
        // Arrêter le scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        services.clear();
        logger.info("Hypervisor arrêté");
    }
    
    /**
     * Enregistre un service
     * @param service Service à enregistrer
     */
    public void registerService(Service service) {
        if (service == null) {
            throw new IllegalArgumentException("Service ne peut pas être null");
        }
        
        String serviceId = service.getId();
        
        if (services.containsKey(serviceId)) {
            logger.warn("Service {} déjà enregistré, remplacement", serviceId);
        }
        
        services.put(serviceId, service);
        logger.info("Service {} enregistré (total: {})", serviceId, services.size());
    }
    
    /**
     * Désenregistre un service
     * @param serviceId ID du service à désenregistrer
     */
    public void unregisterService(String serviceId) {
        Service removed = services.remove(serviceId);
        if (removed != null) {
            logger.info("Service {} désenregistré", serviceId);
            try {
                removed.stop();
            } catch (Exception e) {
                logger.error("Erreur lors de l'arrêt du service {}", serviceId, e);
            }
        } else {
            logger.warn("Service {} non trouvé pour désenregistrement", serviceId);
        }
    }
    
    /**
     * Récupère un service par son ID
     * @param serviceId ID du service
     * @return Service ou null si non trouvé
     */
    public Service getService(String serviceId) {
        return services.get(serviceId);
    }
    
    /**
     * Retourne tous les services enregistrés
     * @return Map non modifiable des services
     */
    public Map<String, Service> getAllServices() {
        return Collections.unmodifiableMap(services);
    }
    
    /**
     * Retourne le statut de tous les services
     * @return Map serviceId -> statut
     */
    public Map<String, Map<String, Object>> getServicesStatus() {
        Map<String, Map<String, Object>> statusMap = new HashMap<>();
        
        for (Map.Entry<String, Service> entry : services.entrySet()) {
            String serviceId = entry.getKey();
            Service service = entry.getValue();
            
            Map<String, Object> status = new HashMap<>();
            status.put("id", serviceId);
            status.put("name", service.getName());
            status.put("state", service.getState());
            status.put("alive", service.isAlive());
            status.put("lastHeartbeat", service.getLastHeartbeat());
            status.put("uptime", service.getUptime());
            
            statusMap.put(serviceId, status);
        }
        
        return statusMap;
    }
    
    /**
     * Vérifie les heartbeats de tous les services
     */
    private void checkHeartbeats() {
        if (!running) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        for (Service service : services.values()) {
            try {
                long lastHeartbeat = service.getLastHeartbeat();
                long timeSinceHeartbeat = currentTime - lastHeartbeat;
                
                if (timeSinceHeartbeat > heartbeatTimeout) {
                    logger.warn("Service {} n'a pas répondu depuis {}ms (timeout: {}ms)",
                        service.getId(), timeSinceHeartbeat, heartbeatTimeout);
                    
                    // Marquer comme non vivant
                    service.markAsUnhealthy();
                    
                    // Tenter de redémarrer si configuré
                    if (service.isAutoRestart()) {
                        logger.info("Tentative de redémarrage du service {}", service.getId());
                        restartService(service);
                    }
                } else {
                    // Service OK
                    if (!service.isAlive()) {
                        logger.info("Service {} est de nouveau vivant", service.getId());
                        service.markAsHealthy();
                    }
                }
            } catch (Exception e) {
                logger.error("Erreur lors de la vérification du service {}", 
                    service.getId(), e);
            }
        }
    }
    
    /**
     * Redémarre un service
     * @param service Service à redémarrer
     */
    private void restartService(Service service) {
        try {
            logger.info("Redémarrage du service {}...", service.getId());
            service.stop();
            Thread.sleep(1000); // Attendre 1s
            service.start();
            logger.info("Service {} redémarré avec succès", service.getId());
        } catch (Exception e) {
            logger.error("Échec du redémarrage du service {}", service.getId(), e);
        }
    }
    
    /**
     * Retourne le nombre de services enregistrés
     * @return Nombre de services
     */
    public int getServiceCount() {
        return services.size();
    }
    
    /**
     * Retourne le nombre de services vivants
     * @return Nombre de services vivants
     */
    public int getAliveServiceCount() {
        return (int) services.values().stream()
            .filter(Service::isAlive)
            .count();
    }
    
    /**
     * Vérifie si l'hypervisor est en cours d'exécution
     * @return true si en cours d'exécution
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Retourne l'intervalle de vérification
     * @return Intervalle en ms
     */
    public long getHealthCheckInterval() {
        return healthCheckInterval;
    }
    
    /**
     * Retourne le timeout du heartbeat
     * @return Timeout en ms
     */
    public long getHeartbeatTimeout() {
        return heartbeatTimeout;
    }
    
    @Override
    public String toString() {
        return String.format("Hypervisor{services=%d, alive=%d, running=%s}",
            getServiceCount(), getAliveServiceCount(), running);
    }
}