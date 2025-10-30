package com.example.akkajr.core;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public abstract class Service implements Heartbeat {
    
    // ========== ATTRIBUTS EXISTANTS ==========
    protected final String id;
    protected String name;
    protected ServiceState state;
    protected List<String> inputsCommands;
    protected LocalDateTime creationTime;
    protected LocalDateTime lastModifiedTime;
    protected Map<String, String> configuration;
    protected Logger logger;
    
    // ========== ATTRIBUTS HEARTBEAT ==========
    private volatile LocalDateTime lastHeartbeat;
    private int heartbeatTimeoutSeconds = 30;  // Timeout par défaut
    private ScheduledExecutorService heartbeatScheduler;
    private final Object heartbeatLock = new Object();
    
    // ========== SNAPSHOT POUR RÉCUPÉRATION ==========
    private ServiceSnapshot lastSnapshot;
    
    public enum ServiceState {
        CREATED, STARTING, RUNNING, PAUSED, 
        STOPPING, STOPPED, ERROR, ZOMBIE  // ZOMBIE = pas de heartbeat
    }
    
    // ========== CONSTRUCTEUR ==========
    protected Service(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.state = ServiceState.CREATED;
        this.inputsCommands = new CopyOnWriteArrayList<>();  // Thread-safe
        this.configuration = new ConcurrentHashMap<>();      // Thread-safe
        this.creationTime = LocalDateTime.now();
        this.lastModifiedTime = LocalDateTime.now();
        this.logger = Logger.getLogger(this.getClass().getName());
        this.lastHeartbeat = LocalDateTime.now();
        
        logger.info("Service créé : " + name + " [" + id + "]");
    }
    
    // ========== MÉTHODES ABSTRAITES ==========
    protected abstract void onStart() throws Exception;
    protected abstract void onStop() throws Exception;
    public abstract void execute() throws Exception;
    protected abstract boolean validateConfiguration();
    
    // ========== CYCLE DE VIE AVEC HEARTBEAT ==========
    
    public final void start() throws Exception {
        if (state == ServiceState.RUNNING) {
            logger.warning("Le service est déjà en cours d'exécution");
            return;
        }
        
        logger.info("Démarrage du service : " + name);
        state = ServiceState.STARTING;
        
        try {
            if (!validateConfiguration()) {
                throw new IllegalStateException("Configuration invalide");
            }
            
            beforeStart();
            onStart();
            
            // Démarre le heartbeat automatique
            startHeartbeatScheduler();
            
            state = ServiceState.RUNNING;
            lastModifiedTime = LocalDateTime.now();
            ping();  // Premier heartbeat
            
            afterStart();
            logger.info("Service démarré avec succès : " + name);
            
        } catch (Exception e) {
            state = ServiceState.ERROR;
            logger.severe("Erreur au démarrage : " + e.getMessage());
            throw e;
        }
    }
    
    public final void stop() throws Exception {
        if (state != ServiceState.RUNNING && state != ServiceState.PAUSED) {
            logger.warning("Le service n'est pas en cours d'exécution");
            return;
        }
        
        logger.info("Arrêt du service : " + name);
        state = ServiceState.STOPPING;
        
        try {
            // Arrête le heartbeat
            stopHeartbeatScheduler();
            
            beforeStop();
            onStop();
            
            state = ServiceState.STOPPED;
            lastModifiedTime = LocalDateTime.now();
            
            afterStop();
            logger.info("Service arrêté : " + name);
            
        } catch (Exception e) {
            state = ServiceState.ERROR;
            logger.severe("Erreur à l'arrêt : " + e.getMessage());
            throw e;
        }
    }
    
    // ========== IMPLÉMENTATION HEARTBEAT ==========
    
    @Override
    public void ping() {
        synchronized (heartbeatLock) {
            lastHeartbeat = LocalDateTime.now();
            logger.fine("💓 Heartbeat : " + name + " [" + id + "]");
        }
    }
    
    @Override
    public LocalDateTime getLastHeartbeat() {
        synchronized (heartbeatLock) {
            return lastHeartbeat;
        }
    }
    
    @Override
    public boolean isAlive() {
        synchronized (heartbeatLock) {
            if (lastHeartbeat == null) return false;
            
            Duration duration = Duration.between(lastHeartbeat, LocalDateTime.now());
            boolean alive = duration.getSeconds() < heartbeatTimeoutSeconds;
            
            if (!alive && state == ServiceState.RUNNING) {
                logger.warning("⚠️ Service ZOMBIE détecté : " + name);
                state = ServiceState.ZOMBIE;
            }
            
            return alive;
        }
    }
    
    @Override
    public void setHeartbeatTimeout(int seconds) {
        this.heartbeatTimeoutSeconds = seconds;
    }
    
    /**
     * Démarre le scheduler qui envoie des heartbeats automatiques
     */
    private void startHeartbeatScheduler() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            return;
        }
        
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Heartbeat-" + name);
            t.setDaemon(true);
            return t;
        });
        
        // Envoie un heartbeat toutes les 5 secondes
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                if (state == ServiceState.RUNNING) {
                    ping();
                }
            } catch (Exception e) {
                logger.severe("Erreur dans le heartbeat : " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
        
        logger.info("Heartbeat scheduler démarré pour : " + name);
    }
    
    private void stopHeartbeatScheduler() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdown();
            try {
                heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                heartbeatScheduler.shutdownNow();
            }
        }
    }
    
    // ========== SNAPSHOT POUR RÉCUPÉRATION ==========
    
    /**
     * Crée un snapshot de l'état actuel du service
     */
    public ServiceSnapshot createSnapshot() {
        ServiceSnapshot snapshot = new ServiceSnapshot();
        snapshot.serviceId = this.id;
        snapshot.serviceName = this.name;
        snapshot.serviceClass = this.getClass().getName();
        snapshot.state = this.state;
        snapshot.pendingCommands = new ArrayList<>(this.inputsCommands);
        snapshot.configuration = new HashMap<>(this.configuration);
        snapshot.snapshotTime = LocalDateTime.now();
        
        this.lastSnapshot = snapshot;
        return snapshot;
    }
    
    /**
     * Restaure l'état depuis un snapshot
     */
    public void restoreFromSnapshot(ServiceSnapshot snapshot) {
        if (snapshot == null) return;
        
        logger.info("Restauration depuis snapshot : " + snapshot.snapshotTime);
        
        this.inputsCommands.clear();
        this.inputsCommands.addAll(snapshot.pendingCommands);
        
        this.configuration.clear();
        this.configuration.putAll(snapshot.configuration);
        
        logger.info("Commandes restaurées : " + inputsCommands.size());
    }
    
    public ServiceSnapshot getLastSnapshot() {
        return lastSnapshot;
    }
    
    // ========== HOOKS ==========
    protected void beforeStart() throws Exception {}
    protected void afterStart() throws Exception {}
    protected void beforeStop() throws Exception {}
    protected void afterStop() throws Exception {}
    
    // ========== COMMANDES ==========
    public void addCommand(String command) {
        if (command != null && !command.trim().isEmpty()) {
            inputsCommands.add(command);
            lastModifiedTime = LocalDateTime.now();
            
            // Crée un snapshot automatique
            createSnapshot();
        }
    }
    
    public void clearCommands() {
        inputsCommands.clear();
    }
    
    // ========== GETTERS ==========
    public String getId() { return id; }
    public String getName() { return name; }
    public ServiceState getState() { return state; }
    public boolean isRunning() { return state == ServiceState.RUNNING; }
    public List<String> getInputsCommands() { return new ArrayList<>(inputsCommands); }
    
    @Override
    public String toString() {
        return String.format("Service[id=%s, name=%s, state=%s, alive=%s]",
                           id, name, state, isAlive());
    }
}