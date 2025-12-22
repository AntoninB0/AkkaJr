package com.example.akkajr.core;

/**
 * Service de test pour démonstration
 */
public class TestService extends Service {
    
    private Integer heartbeatTimeout;
    private Boolean crashMode;
    
    public TestService(String id) {
        super(id);
        this.heartbeatTimeout = 10000; // 10 secondes par défaut
        this.crashMode = false;
    }
    
    @Override
    public void start() throws Exception {
        logger.info("Démarrage du TestService {}", id);
        setState(ServiceState.STARTING);
        
        startTime.set(System.currentTimeMillis());
        alive.set(true);
        updateHeartbeat();
        
        setState(ServiceState.RUNNING);
        logger.info("TestService {} démarré", id);
    }
    
    @Override
    public void stop() throws Exception {
        logger.info("Arrêt du TestService {}", id);
        setState(ServiceState.STOPPING);
        
        alive.set(false);
        
        setState(ServiceState.STOPPED);
        logger.info("TestService {} arrêté", id);
    }
    
    @Override
    public void execute() throws Exception {
        logger.info("Exécution de {} commandes pour {}", commands.size(), id);
        
        for (String command : commands) {
            logger.info("Exécution: {}", command);
            // Simulation d'exécution
            Thread.sleep(100);
        }
        
        logger.info("Exécution terminée pour {}", id);
    }
    
    // Getters/Setters spécifiques
    
    public Integer getHeartbeatTimeout() {
        return heartbeatTimeout;
    }
    
    public void setHeartbeatTimeout(Integer heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
    }
    
    public Boolean getCrashMode() {
        return crashMode;
    }
    
    public void setCrashMode(Boolean crashMode) {
        this.crashMode = crashMode;
    }
}