package com.example.akkajr.core;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Circuit Breaker : protège contre les défaillances en cascade
 * 
 * États :
 * - CLOSED : Tout fonctionne, les appels passent
 * - OPEN : Trop d'échecs, les appels sont bloqués
 * - HALF_OPEN : Test si le service est revenu, quelques appels passent
 */
public class CircuitBreaker {
    
    private static final Logger LOGGER = Logger.getLogger(CircuitBreaker.class.getName());
    
    private final String name;
    private final int maxFailures;
    private final Duration timeout;
    private final Duration resetTimeout;
    
    private final AtomicInteger failureCount;
    private final AtomicInteger successCount;
    private final AtomicReference<State> state;
    private volatile LocalDateTime lastFailureTime;
    private volatile LocalDateTime openedAt;
    
    public enum State {
        CLOSED,      // Normal : les requêtes passent
        OPEN,        // Circuit ouvert : les requêtes sont rejetées
        HALF_OPEN    // Test : quelques requêtes passent pour tester
    }
    
    /**
     * Crée un Circuit Breaker
     * 
     * @param name Nom du circuit breaker
     * @param maxFailures Nombre d'échecs avant ouverture
     * @param timeout Temps avant de tester la récupération
     * @param resetTimeout Temps avant de réinitialiser le compteur d'échecs
     */
    public CircuitBreaker(String name, int maxFailures, Duration timeout, Duration resetTimeout) {
        this.name = name;
        this.maxFailures = maxFailures;
        this.timeout = timeout;
        this.resetTimeout = resetTimeout;
        
        this.failureCount = new AtomicInteger(0);
        this.successCount = new AtomicInteger(0);
        this.state = new AtomicReference<>(State.CLOSED);
        this.lastFailureTime = null;
        this.openedAt = null;
    }
    
    /**
     * Configuration par défaut : 5 échecs, 30s timeout, 5min reset
     */
    public static CircuitBreaker withDefaults(String name) {
        return new CircuitBreaker(name, 5, Duration.ofSeconds(30), Duration.ofMinutes(5));
    }
    
    /**
     * Configuration stricte : 3 échecs, 10s timeout, 1min reset
     */
    public static CircuitBreaker strict(String name) {
        return new CircuitBreaker(name, 3, Duration.ofSeconds(10), Duration.ofMinutes(1));
    }
    
    /**
     * Configuration permissive : 10 échecs, 1min timeout, 10min reset
     */
    public static CircuitBreaker permissive(String name) {
        return new CircuitBreaker(name, 10, Duration.ofMinutes(1), Duration.ofMinutes(10));
    }
    
    /**
     * Vérifie si l'appel peut passer
     */
    public boolean allowRequest() {
        State currentState = state.get();
        
        switch (currentState) {
            case CLOSED:
                // Vérifie si on doit réinitialiser le compteur
                if (shouldResetFailureCount()) {
                    reset();
                }
                return true;
                
            case OPEN:
                // Vérifie si on peut passer en HALF_OPEN
                if (shouldAttemptReset()) {
                    LOGGER.info("🔶 Circuit Breaker " + name + " -> HALF_OPEN (test de récupération)");
                    state.set(State.HALF_OPEN);
                    successCount.set(0);
                    return true;
                }
                return false;
                
            case HALF_OPEN:
                // En mode test, on laisse passer quelques requêtes
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * Enregistre un succès
     */
    public void recordSuccess() {
        State currentState = state.get();
        
        if (currentState == State.HALF_OPEN) {
            int successes = successCount.incrementAndGet();
            
            // Si on a assez de succès, on referme le circuit
            if (successes >= 3) {
                LOGGER.info("🟢 Circuit Breaker " + name + " -> CLOSED (récupération réussie)");
                reset();
            }
        } else if (currentState == State.CLOSED) {
            // Réinitialise progressivement les échecs
            if (failureCount.get() > 0) {
                failureCount.decrementAndGet();
            }
        }
    }
    
    /**
     * Enregistre un échec
     */
    public void recordFailure() {
        lastFailureTime = LocalDateTime.now();
        int failures = failureCount.incrementAndGet();
        
        State currentState = state.get();
        
        if (currentState == State.HALF_OPEN) {
            // Un échec en HALF_OPEN : on réouvre le circuit
            LOGGER.warning("🔴 Circuit Breaker " + name + " -> OPEN (échec pendant test)");
            state.set(State.OPEN);
            openedAt = LocalDateTime.now();
            
        } else if (currentState == State.CLOSED && failures >= maxFailures) {
            // Trop d'échecs : on ouvre le circuit
            LOGGER.warning("🔴 Circuit Breaker " + name + " -> OPEN (" + failures + " échecs)");
            state.set(State.OPEN);
            openedAt = LocalDateTime.now();
        }
    }
    
    /**
     * Exécute une opération avec protection du circuit breaker
     */
    public <T> T execute(SupplierWithException<T> operation) throws Exception {
        if (!allowRequest()) {
            throw new CircuitBreakerOpenException(
                "Circuit breaker " + name + " is OPEN - request rejected"
            );
        }
        
        try {
            T result = operation.get();
            recordSuccess();
            return result;
            
        } catch (Exception e) {
            recordFailure();
            throw e;
        }
    }
    
    /**
     * Exécute une opération sans valeur de retour
     */
    public void execute(RunnableWithException operation) throws Exception {
        if (!allowRequest()) {
            throw new CircuitBreakerOpenException(
                "Circuit breaker " + name + " is OPEN - request rejected"
            );
        }
        
        try {
            operation.run();
            recordSuccess();
            
        } catch (Exception e) {
            recordFailure();
            throw e;
        }
    }
    
    /**
     * Réinitialise le circuit breaker
     */
    public void reset() {
        state.set(State.CLOSED);
        failureCount.set(0);
        successCount.set(0);
        openedAt = null;
        LOGGER.info("🔄 Circuit Breaker " + name + " réinitialisé");
    }
    
    /**
     * Force l'ouverture du circuit
     */
    public void forceOpen() {
        state.set(State.OPEN);
        openedAt = LocalDateTime.now();
        LOGGER.warning("⚠️ Circuit Breaker " + name + " forcé en OPEN");
    }
    
    /**
     * Vérifie si on doit tenter une récupération
     */
    private boolean shouldAttemptReset() {
        if (openedAt == null) return false;
        
        Duration elapsed = Duration.between(openedAt, LocalDateTime.now());
        return elapsed.compareTo(timeout) >= 0;
    }
    
    /**
     * Vérifie si on doit réinitialiser le compteur d'échecs
     */
    private boolean shouldResetFailureCount() {
        if (lastFailureTime == null || failureCount.get() == 0) {
            return false;
        }
        
        Duration elapsed = Duration.between(lastFailureTime, LocalDateTime.now());
        return elapsed.compareTo(resetTimeout) >= 0;
    }
    
    // ========== GETTERS ==========
    
    public State getState() {
        return state.get();
    }
    
    public int getFailureCount() {
        return failureCount.get();
    }
    
    public boolean isOpen() {
        return state.get() == State.OPEN;
    }
    
    public boolean isClosed() {
        return state.get() == State.CLOSED;
    }
    
    public boolean isHalfOpen() {
        return state.get() == State.HALF_OPEN;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Retourne des statistiques
     */
    public CircuitBreakerStats getStats() {
        return new CircuitBreakerStats(
            name,
            state.get(),
            failureCount.get(),
            maxFailures,
            openedAt,
            lastFailureTime
        );
    }
    
    @Override
    public String toString() {
        return String.format("CircuitBreaker[name=%s, state=%s, failures=%d/%d]",
                           name, state.get(), failureCount.get(), maxFailures);
    }
    
    // ========== INTERFACES FONCTIONNELLES ==========
    
    @FunctionalInterface
    public interface SupplierWithException<T> {
        T get() throws Exception;
    }
    
    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Exception;
    }
    
    // ========== EXCEPTION ==========
    
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
    
    // ========== STATS ==========
    
    public static class CircuitBreakerStats {
        public final String name;
        public final State state;
        public final int failureCount;
        public final int maxFailures;
        public final LocalDateTime openedAt;
        public final LocalDateTime lastFailureTime;
        
        public CircuitBreakerStats(String name, State state, int failureCount,
                                  int maxFailures, LocalDateTime openedAt,
                                  LocalDateTime lastFailureTime) {
            this.name = name;
            this.state = state;
            this.failureCount = failureCount;
            this.maxFailures = maxFailures;
            this.openedAt = openedAt;
            this.lastFailureTime = lastFailureTime;
        }
        
        public boolean isHealthy() {
            return state == State.CLOSED && failureCount == 0;
        }
        
        @Override
        public String toString() {
            return String.format("%s: %s (%d/%d failures)", 
                               name, state, failureCount, maxFailures);
        }
    }
}