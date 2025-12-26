package com.example.akkajr.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hypervisor - Gestionnaire central des services
 * Surveille l'état des services et gère leur cycle de vie
 */
public class Hypervisor {

    private static final Logger logger = LoggerFactory.getLogger(Hypervisor.class);

    private final Map<String, Service> services;
    private final ScheduledExecutorService scheduler;
    private final long healthCheckInterval;
    private final long heartbeatTimeout;
    private volatile boolean running;

    public Hypervisor(long healthCheckInterval) {
        this.services = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.healthCheckInterval = healthCheckInterval;
        this.heartbeatTimeout = healthCheckInterval * 3;
        this.running = false;
        logger.info("Hypervisor créé avec intervalle de {}ms", healthCheckInterval);
    }

    public void start() {
        if (running) {
            logger.warn("Hypervisor déjà démarré");
            return;
        }

        running = true;
        logger.info("Démarrage de l'Hypervisor...");

        scheduler.scheduleAtFixedRate(this::checkHeartbeats, healthCheckInterval, healthCheckInterval, TimeUnit.MILLISECONDS);

        logger.info("Hypervisor démarré avec succès");
    }

    public void stop() {
        if (!running) {
            logger.warn("Hypervisor déjà arrêté");
            return;
        }

        running = false;
        logger.info("Arrêt de l'Hypervisor...");

        services.values().forEach(service -> {
            try {
                service.stop();
            } catch (Exception e) {
                logger.error("Erreur lors de l'arrêt du service {}", service.getId(), e);
            }
        });

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

    public Service getService(String serviceId) {
        return services.get(serviceId);
    }

    public Map<String, Service> getAllServices() {
        return Collections.unmodifiableMap(services);
    }

    public Map<String, Map<String, Object>> getServicesStatus() {
        Map<String, Map<String, Object>> statusMap = new HashMap<>();
        for (Map.Entry<String, Service> entry : services.entrySet()) {
            Service service = entry.getValue();
            Map<String, Object> status = new HashMap<>();
            status.put("id", entry.getKey());
            status.put("name", service.getName());
            status.put("state", service.getState());
            status.put("alive", service.isAlive());
            status.put("lastHeartbeat", service.getLastHeartbeat());
            status.put("uptime", service.getUptime());
            statusMap.put(entry.getKey(), status);
        }
        return statusMap;
    }

    private void checkHeartbeats() {
        if (!running) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Service service : services.values()) {
            try {
                long since = now - service.getLastHeartbeat();
                if (since > heartbeatTimeout) {
                    logger.warn("Service {} n'a pas répondu depuis {}ms (timeout: {}ms)",
                            service.getId(), since, heartbeatTimeout);
                    service.markAsUnhealthy();
                    if (service.isAutoRestart()) {
                        logger.info("Tentative de redémarrage du service {}", service.getId());
                        restartService(service);
                    }
                } else if (!service.isAlive()) {
                    logger.info("Service {} est de nouveau vivant", service.getId());
                    service.markAsHealthy();
                }
            } catch (Exception e) {
                logger.error("Erreur lors de la vérification du service {}", service.getId(), e);
            }
        }
    }

    private void restartService(Service service) {
        try {
            logger.info("Redémarrage du service {}...", service.getId());
            service.stop();
            Thread.sleep(1000);
            service.start();
            logger.info("Service {} redémarré avec succès", service.getId());
        } catch (Exception e) {
            logger.error("Échec du redémarrage du service {}", service.getId(), e);
        }
    }

    public int getServiceCount() {
        return services.size();
    }

    public int getAliveServiceCount() {
        return (int) services.values().stream().filter(Service::isAlive).count();
    }

    public boolean isRunning() {
        return running;
    }

    public long getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public long getHeartbeatTimeout() {
        return heartbeatTimeout;
    }

    @Override
    public String toString() {
        return String.format("Hypervisor{services=%d, alive=%d, running=%s}",
                getServiceCount(), getAliveServiceCount(), running);
    }
}
