package com.example.akkajr.core;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.logging.*;

public abstract class Service implements Heartbeat, DeathWatch {
    
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
    private int heartbeatTimeoutSeconds = 30;
    private ScheduledExecutorService heartbeatScheduler;
    private final Object heartbeatLock = new Object();
    
    // ========== SNAPSHOT POUR RÉCUPÉRATION ==========
    private ServiceSnapshot lastSnapshot;
    
    // ========== HIÉRARCHIE PARENT-ENFANT ==========
    private Service parent;
    private List<Service> children;
    private SupervisionStrategy supervisionStrategy;
    private final Object childrenLock = new Object();
    
    // ========== RÉSILIENCE : RETRY, CIRCUIT BREAKER, DEATHWATCH ==========
    private RetryPolicy retryPolicy;
    private CircuitBreaker circuitBreaker;
    private Set<Service> watchedServices;
    private final DeathWatchManager deathWatchManager;
    
    public enum ServiceState {
        CREATED, STARTING, RUNNING, PAUSED, 
        STOPPING, STOPPED, ERROR, ZOMBIE
    }
    
    public enum SupervisionStrategy {
        RESTART,      // Redémarre l'enfant en cas d'échec
        STOP,         // Arrête l'enfant définitivement
        RESUME,       // Reprend l'enfant après une pause
        ESCALATE,     // Remonte l'erreur au parent
        IGNORE        // Ignore l'échec
    }
    
    // ========== CONSTRUCTEUR ==========
    protected Service(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.state = ServiceState.CREATED;
        this.inputsCommands = new CopyOnWriteArrayList<>();
        this.configuration = new ConcurrentHashMap<>();
        this.creationTime = LocalDateTime.now();
        this.lastModifiedTime = LocalDateTime.now();
        this.logger = Logger.getLogger(this.getClass().getName());
        this.lastHeartbeat = LocalDateTime.now();
        
        // Initialisation hiérarchie
        this.children = new CopyOnWriteArrayList<>();
        this.parent = null;
        this.supervisionStrategy = SupervisionStrategy.RESTART;
        
        // Initialisation résilience
        this.retryPolicy = RetryPolicy.defaultPolicy();
        this.circuitBreaker = CircuitBreaker.withDefaults(name);
        this.watchedServices = ConcurrentHashMap.newKeySet();
        this.deathWatchManager = DeathWatchManager.getInstance();
        
        logger.info("Service créé : " + name + " [" + id + "]");
    }
    
    // ========== MÉTHODES ABSTRAITES ==========
    protected abstract void onStart() throws Exception;
    protected abstract void onStop() throws Exception;
    public abstract void execute() throws Exception;
    protected abstract boolean validateConfiguration();
    
    // ========== GESTION HIÉRARCHIE PARENT-ENFANT ==========
    
    public void addChild(Service child) {
        if (child == null) {
            throw new IllegalArgumentException("L'enfant ne peut pas être null");
        }
        
        if (child == this) {
            throw new IllegalArgumentException("Un service ne peut pas être son propre enfant");
        }
        
        synchronized (childrenLock) {
            if (child.parent != null) {
                child.parent.removeChild(child);
            }
            
            children.add(child);
            child.parent = this;
            
            logger.info("Enfant ajouté : " + child.getName() + " -> Parent : " + this.name);
        }
        
        startChildSupervision(child);
    }
    
    public void removeChild(Service child) {
        if (child == null) return;
        
        synchronized (childrenLock) {
            if (children.remove(child)) {
                child.parent = null;
                logger.info("Enfant retiré : " + child.getName() + " de Parent : " + this.name);
            }
        }
    }
    
    public void removeAllChildren() {
        synchronized (childrenLock) {
            for (Service child : children) {
                child.parent = null;
            }
            children.clear();
            logger.info("Tous les enfants ont été retirés de : " + this.name);
        }
    }
    
    public List<Service> getChildren() {
        synchronized (childrenLock) {
            return new ArrayList<>(children);
        }
    }
    
    public Service getParent() {
        return parent;
    }
    
    public boolean hasChildren() {
        synchronized (childrenLock) {
            return !children.isEmpty();
        }
    }
    
    public int getChildrenCount() {
        synchronized (childrenLock) {
            return children.size();
        }
    }
    
    public void setSupervisionStrategy(SupervisionStrategy strategy) {
        this.supervisionStrategy = strategy;
        logger.info("Stratégie de supervision définie : " + strategy + " pour " + this.name);
    }
    
    public SupervisionStrategy getSupervisionStrategy() {
        return supervisionStrategy;
    }
    
    private void startChildSupervision(Service child) {
        Thread supervisorThread = new Thread(() -> {
            while (children.contains(child)) {
                try {
                    Thread.sleep(5000);
                    
                    if (child.getState() == ServiceState.ERROR || 
                        child.getState() == ServiceState.ZOMBIE) {
                        
                        logger.warning("⚠️ Échec détecté sur l'enfant : " + child.getName());
                        handleChildFailure(child);
                    }
                    
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "Supervisor-" + this.name + "-" + child.getName());
        
        supervisorThread.setDaemon(true);
        supervisorThread.start();
    }
    
    private void handleChildFailure(Service child) {
        logger.warning("Application de la stratégie : " + supervisionStrategy + 
                      " pour l'enfant : " + child.getName());
        
        try {
            switch (supervisionStrategy) {
                case RESTART:
                    logger.info("🔄 Redémarrage de l'enfant : " + child.getName());
                    
                    if (child.getLastSnapshot() != null) {
                        child.restoreFromSnapshot(child.getLastSnapshot());
                    }
                    
                    child.stop();
                    Thread.sleep(1000);
                    child.start();
                    break;
                    
                case STOP:
                    logger.info("🛑 Arrêt de l'enfant : " + child.getName());
                    child.stop();
                    removeChild(child);
                    break;
                    
                case RESUME:
                    logger.info("▶️ Reprise de l'enfant : " + child.getName());
                    if (child.getState() == ServiceState.PAUSED || 
                        child.getState() == ServiceState.STOPPED) {
                        child.start();
                    }
                    break;
                    
                case ESCALATE:
                    logger.warning("⬆️ Escalade de l'erreur au parent");
                    if (this.parent != null) {
                        this.state = ServiceState.ERROR;
                        this.parent.handleChildFailure(this);
                    } else {
                        this.state = ServiceState.ERROR;
                        this.stop();
                    }
                    break;
                    
                case IGNORE:
                    logger.info("🤷 Ignorance de l'échec de l'enfant : " + child.getName());
                    break;
            }
        } catch (Exception e) {
            logger.severe("Erreur lors de la gestion de l'échec de l'enfant : " + e.getMessage());
        }
    }
    
    // ========== CYCLE DE VIE AVEC HEARTBEAT ET RÉSILIENCE ==========
    
    public final void start() throws Exception {
        if (state == ServiceState.RUNNING) {
            logger.warning("Le service est déjà en cours d'exécution");
            return;
        }
        
        // Vérifie le circuit breaker
        if (circuitBreaker.isOpen()) {
            logger.severe("❌ Circuit breaker OUVERT pour : " + name);
            throw new IllegalStateException("Circuit breaker is OPEN for " + name);
        }
        
        // Vérifie la retry policy
        if (!retryPolicy.canRetry()) {
            logger.severe("❌ Limite de retry atteinte pour : " + name);
            logger.severe("   Statistiques : " + retryPolicy.getStats());
            throw new IllegalStateException("Retry limit exceeded for " + name);
        }
        
        logger.info("Démarrage du service : " + name);
        logger.info("   Retry : " + retryPolicy.getStats());
        logger.info("   Circuit Breaker : " + circuitBreaker.getState());
        
        state = ServiceState.STARTING;
        
        try {
            if (!validateConfiguration()) {
                throw new IllegalStateException("Configuration invalide");
            }
            
            beforeStart();
            onStart();
            
            startHeartbeatScheduler();
            
            state = ServiceState.RUNNING;
            lastModifiedTime = LocalDateTime.now();
            ping();
            
            startAllChildren();
            
            afterStart();
            
            // Enregistre le succès
            retryPolicy.recordRetry();
            circuitBreaker.recordSuccess();
            
            logger.info("✅ Service démarré avec succès : " + name);
            
        } catch (Exception e) {
            state = ServiceState.ERROR;
            circuitBreaker.recordFailure();
            logger.severe("❌ Erreur au démarrage : " + e.getMessage());
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
            stopAllChildren();
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
    
    private void startAllChildren() {
        synchronized (childrenLock) {
            for (Service child : children) {
                try {
                    logger.info("Démarrage de l'enfant : " + child.getName());
                    child.start();
                } catch (Exception e) {
                    logger.severe("Impossible de démarrer l'enfant " + child.getName() + 
                                ": " + e.getMessage());
                    handleChildFailure(child);
                }
            }
        }
    }
    
    private void stopAllChildren() {
        synchronized (childrenLock) {
            for (Service child : children) {
                try {
                    logger.info("Arrêt de l'enfant : " + child.getName());
                    child.stop();
                } catch (Exception e) {
                    logger.severe("Erreur lors de l'arrêt de l'enfant " + child.getName() + 
                                ": " + e.getMessage());
                }
            }
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
    
    private void startHeartbeatScheduler() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            return;
        }
        
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Heartbeat-" + name);
            t.setDaemon(true);
            return t;
        });
        
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
    
    // ========== IMPLÉMENTATION DEATHWATCH ==========
    
    @Override
    public void watch(Service service) {
        watchedServices.add(service);
        deathWatchManager.watch(service, this);
        logger.info("👁️ Surveillance activée pour : " + service.getName());
    }
    
    @Override
    public void unwatch(Service service) {
        watchedServices.remove(service);
        deathWatchManager.unwatch(service, this);
        logger.info("👁️ Surveillance désactivée pour : " + service.getName());
    }
    
    @Override
    public void onServiceTerminated(Service terminatedService) {
        logger.warning("☠️ Service surveillé mort : " + terminatedService.getName());
        handleWatchedServiceDeath(terminatedService);
    }
    
    @Override
    public Set<Service> getWatchedServices() {
        return new HashSet<>(watchedServices);
    }
    
    /**
     * Gère la mort d'un service surveillé (peut être surchargé)
     */
    protected void handleWatchedServiceDeath(Service deadService) {
        logger.warning("⚰️ Traitement de la mort de : " + deadService.getName());
        // Implémentation par défaut : peut être surchargée dans les sous-classes
    }
    
    // ========== GESTION RETRY POLICY ==========
    
    public void setRetryPolicy(RetryPolicy policy) {
        this.retryPolicy = policy;
        logger.info("Retry policy définie : " + policy);
    }
    
    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }
    
    public void resetRetryPolicy() {
        retryPolicy.reset();
        logger.info("Retry policy réinitialisée pour : " + name);
    }
    
    // ========== GESTION CIRCUIT BREAKER ==========
    
    public void setCircuitBreaker(CircuitBreaker breaker) {
        this.circuitBreaker = breaker;
        logger.info("Circuit breaker défini : " + breaker);
    }
    
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }
    
    public void resetCircuitBreaker() {
        circuitBreaker.reset();
        logger.info("Circuit breaker réinitialisé pour : " + name);
    }
    
    /**
     * Exécute une opération avec circuit breaker
     */
    protected <T> T executeWithCircuitBreaker(Supplier<T> operation) throws Exception {
        return circuitBreaker.execute(() -> operation.get());
    }
    
    /**
     * Exécute une opération void avec circuit breaker
     */
    protected void executeWithCircuitBreaker(Runnable operation) throws Exception {
        circuitBreaker.execute(() -> {
            operation.run();
        });
    }
    
    // ========== SNAPSHOT POUR RÉCUPÉRATION ==========
    
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
    
    /**
     * Retourne un statut complet du service avec toutes ses métriques
     */
    public ServiceStatus getFullStatus() {
        return new ServiceStatus(
            id, name, state, isAlive(),
            getChildrenCount(), watchedServices.size(),
            retryPolicy.getStats(), circuitBreaker.getStats()
        );
    }
    
    @Override
    public String toString() {
        return String.format("Service[id=%s, name=%s, state=%s, alive=%s, children=%d, watched=%d, cb=%s]",
                           id, name, state, isAlive(), getChildrenCount(), 
                           watchedServices.size(), circuitBreaker.getState());
    }
    
    /**
     * Classe interne pour le statut complet
     */
    public static class ServiceStatus {
        public final String id;
        public final String name;
        public final ServiceState state;
        public final boolean alive;
        public final int childrenCount;
        public final int watchedServicesCount;
        public final RetryPolicy.RetryStats retryStats;
        public final CircuitBreaker.CircuitBreakerStats circuitBreakerStats;
        
        public ServiceStatus(String id, String name, ServiceState state, boolean alive,
                           int childrenCount, int watchedServicesCount,
                           RetryPolicy.RetryStats retryStats,
                           CircuitBreaker.CircuitBreakerStats circuitBreakerStats) {
            this.id = id;
            this.name = name;
            this.state = state;
            this.alive = alive;
            this.childrenCount = childrenCount;
            this.watchedServicesCount = watchedServicesCount;
            this.retryStats = retryStats;
            this.circuitBreakerStats = circuitBreakerStats;
        }
        
        @Override
        public String toString() {
            return String.format(
                "ServiceStatus[\n" +
                "  name=%s\n" +
                "  state=%s\n" +
                "  alive=%s\n" +
                "  children=%d\n" +
                "  watched=%d\n" +
                "  %s\n" +
                "  %s\n" +
                "]",
                name, state, alive, childrenCount, watchedServicesCount,
                retryStats, circuitBreakerStats
            );
        }
    }
}