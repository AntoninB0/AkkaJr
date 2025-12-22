package com.example.akkajr.core;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Interface de surveillance (DeathWatch)
 */
public interface DeathWatch {
    void watch(Service service);
    void unwatch(Service service);
    void onServiceTerminated(Service terminatedService);
    Set<Service> getWatchedServices();
}

/**
 * Gestionnaire DeathWatch corrigé
 */
class DeathWatchManager {
    private static final Logger LOGGER = Logger.getLogger(DeathWatchManager.class.getName());
    private final Map<Service, Set<DeathWatch>> watchers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean running = false;

    public void start() {
        if (running) return;
        running = true;
        // Utilise l'état FAILED défini dans ton enum ServiceState
        scheduler.scheduleAtFixedRate(() -> {
            watchers.forEach((service, serviceWatchers) -> {
                if (service.getState() == Service.ServiceState.FAILED || !service.isAlive()) {
                    serviceWatchers.forEach(w -> w.onServiceTerminated(service));
                    watchers.remove(service);
                }
            });
        }, 0, 2, TimeUnit.SECONDS);
    }
}