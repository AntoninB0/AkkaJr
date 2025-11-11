package com.example.akkajr.core;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * Système de surveillance des services (DeathWatch)
 * Permet de surveiller et d'être notifié de la mort d'un service
 */
public interface DeathWatch {
    
    /**
     * Surveille un service (watch)
     * Recevra une notification si le service meurt
     */
    void watch(Service service);
    
    /**
     * Arrête de surveiller un service (unwatch)
     */
    void unwatch(Service service);
    
    /**
     * Appelé quand un service surveillé meurt
     */
    void onServiceTerminated(Service terminatedService);
    
    /**
     * Retourne la liste des services surveillés
     */
    Set<Service> getWatchedServices();
}

/**
 * Gestionnaire de DeathWatch
 * Centralise la surveillance de tous les services
 */
class DeathWatchManager {
    
    private static final Logger LOGGER = Logger.getLogger(DeathWatchManager.class.getName());
    
    // Map: Service surveillé -> Liste des watchers
    private final Map<Service, Set<DeathWatch>> watchers;
    
    // Scheduler pour vérifier périodiquement
    private final ScheduledExecutorService scheduler;
    
    private volatile boolean running;
    
    public DeathWatchManager() {
        this.watchers = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DeathWatch-Monitor");
            t.setDaemon(true);
            return t;
        });
        this.running = false;
    }
    
    /**
     * Démarre la surveillance
     */
    public void start() {
        if (running) return;
        
        running = true;
        LOGGER.info("👁️ DeathWatch Manager démarré");
        
        // Vérifie toutes les 2 secondes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkWatchedServices();
            } catch (Exception e) {
                LOGGER.severe("Erreur dans DeathWatch: " + e.getMessage());
            }
        }, 0, 2, TimeUnit.SECONDS);
    }
    
    /**
     * Arrête la surveillance
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        LOGGER.info("👁️ DeathWatch Manager arrêté");
    }
    
    /**
     * Enregistre un watcher pour un service
     */
    public void watch(Service service, DeathWatch watcher) {
        watchers.computeIfAbsent(service, k -> ConcurrentHashMap.newKeySet())
                .add(watcher);
        LOGGER.fine("👁️ Surveillance ajoutée: " + service.getName());
    }
    
    /**
     * Retire un watcher
     */
    public void unwatch(Service service, DeathWatch watcher) {
        Set<DeathWatch> serviceWatchers = watchers.get(service);
        if (serviceWatchers != null) {
            serviceWatchers.remove(watcher);
            if (serviceWatchers.isEmpty()) {
                watchers.remove(service);
            }
        }
        LOGGER.fine("👁️ Surveillance retirée: " + service.getName());
    }
    
    /**
     * Vérifie tous les services surveillés
     */
    private void checkWatchedServices() {
        for (Map.Entry<Service, Set<DeathWatch>> entry : watchers.entrySet()) {
            Service service = entry.getKey();
            
            // Vérifie si le service est mort
            if (service.getState() == Service.ServiceState.STOPPED ||
                service.getState() == Service.ServiceState.ERROR ||
                !service.isAlive()) {
                
                LOGGER.warning("☠️ Service mort détecté: " + service.getName());
                
                // Notifie tous les watchers
                for (DeathWatch watcher : entry.getValue()) {
                    try {
                        watcher.onServiceTerminated(service);
                    } catch (Exception e) {
                        LOGGER.severe("Erreur dans notification: " + e.getMessage());
                    }
                }
                
                // Retire de la surveillance
                watchers.remove(service);
            }
        }
    }
    
    /**
     * Retourne le nombre de services surveillés
     */
    public int getWatchCount() {
        return watchers.size();
    }
    
    /**
     * Singleton
     */
    private static DeathWatchManager instance;
    
    public static synchronized DeathWatchManager getInstance() {
        if (instance == null) {
            instance = new DeathWatchManager();
            instance.start();
        }
        return instance;
    }
}