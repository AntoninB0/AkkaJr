package com.example.akkajr.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Classe de base pour tous les services
 */
public abstract class Service {
    
    protected static final Logger logger = LoggerFactory.getLogger(Service.class);
    
    protected final String id;
    protected String name;
    protected volatile ServiceState state;
    protected final AtomicBoolean alive;
    protected final AtomicLong lastHeartbeat;
    protected final AtomicLong startTime;
    protected boolean autoRestart;
    protected List<String> commands;
    
    /**
     * Constructeur
     * @param id ID unique du service
     */
    public Service(String id) {
        this.id = id;
        this.name = id;
        this.state = ServiceState.STOPPED;
        this.alive = new AtomicBoolean(false);
        this.lastHeartbeat = new AtomicLong(System.currentTimeMillis());
        this.startTime = new AtomicLong(0);
        this.autoRestart = false;
        this.commands = new ArrayList<>();
    }
    
    // ========== MÉTHODES ABSTRAITES ==========
    
    /**
     * Démarre le service (à implémenter)
     */
    public abstract void start() throws Exception;
    
    /**
     * Arrête le service (à implémenter)
     */
    public abstract void stop() throws Exception;
    
    /**
     * Exécute les commandes du service (à implémenter)
     */
    public abstract void execute() throws Exception;
    
    // ========== GETTERS/SETTERS ==========
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public ServiceState getState() {
        return state;
    }
    
    protected void setState(ServiceState state) {
        this.state = state;
    }
    
    public boolean isAlive() {
        return alive.get();
    }
    
    public void markAsHealthy() {
        alive.set(true);
        updateHeartbeat();
    }
    
    public void markAsUnhealthy() {
        alive.set(false);
    }
    
    public long getLastHeartbeat() {
        return lastHeartbeat.get();
    }
    
    public void updateHeartbeat() {
        lastHeartbeat.set(System.currentTimeMillis());
    }
    
    public long getUptime() {
        if (startTime.get() == 0) {
            return 0;
        }
        return System.currentTimeMillis() - startTime.get();
    }
    
    public boolean isAutoRestart() {
        return autoRestart;
    }
    
    public void setAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
    }
    
    public void addCommand(String command) {
        this.commands.add(command);
    }
    
    public List<String> getInputsCommands() {
        return Collections.unmodifiableList(commands);
    }
    
    public void clearCommands() {
        this.commands.clear();
    }
    
    @Override
    public String toString() {
        return String.format("Service{id='%s', name='%s', state=%s, alive=%s}",
            id, name, state, alive.get());
    }
    
    // ========== ENUM ==========
    
    public enum ServiceState {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING,
        FAILED
    }
}