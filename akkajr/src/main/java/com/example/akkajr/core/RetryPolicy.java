package com.example.akkajr.core;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Politique de retry : limite le nombre de redémarrages dans une période
 * Inspiré du BackoffSupervisor d'Akka
 */
public class RetryPolicy {
    
    private final int maxRetries;
    private final Duration withinDuration;
    private final Duration backoffDelay;
    private final Deque<LocalDateTime> retryHistory;
    
    /**
     * Crée une politique de retry
     * 
     * @param maxRetries Nombre maximum de tentatives
     * @param withinDuration Dans quelle période (ex: 5 tentatives en 1 minute)
     * @param backoffDelay Délai d'attente entre chaque tentative
     */
    public RetryPolicy(int maxRetries, Duration withinDuration, Duration backoffDelay) {
        this.maxRetries = maxRetries;
        this.withinDuration = withinDuration;
        this.backoffDelay = backoffDelay;
        this.retryHistory = new ConcurrentLinkedDeque<>();
    }
    
    /**
     * Configuration par défaut : 3 tentatives en 1 minute
     */
    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(3, Duration.ofMinutes(1), Duration.ofSeconds(5));
    }
    
    /**
     * Configuration stricte : 5 tentatives en 10 secondes
     */
    public static RetryPolicy strictPolicy() {
        return new RetryPolicy(5, Duration.ofSeconds(10), Duration.ofSeconds(2));
    }
    
    /**
     * Configuration permissive : 10 tentatives en 5 minutes
     */
    public static RetryPolicy permissivePolicy() {
        return new RetryPolicy(10, Duration.ofMinutes(5), Duration.ofSeconds(10));
    }
    
    /**
     * Vérifie si on peut encore réessayer
     */
    public boolean canRetry() {
        LocalDateTime now = LocalDateTime.now();
        
        // Nettoie l'historique (retire les tentatives trop anciennes)
        cleanOldRetries(now);
        
        // Vérifie si on a dépassé la limite
        return retryHistory.size() < maxRetries;
    }
    
    /**
     * Enregistre une tentative
     */
    public void recordRetry() {
        retryHistory.add(LocalDateTime.now());
    }
    
    /**
     * Réinitialise l'historique
     */
    public void reset() {
        retryHistory.clear();
    }
    
    /**
     * Retourne le nombre de tentatives dans la période actuelle
     */
    public int getCurrentRetryCount() {
        cleanOldRetries(LocalDateTime.now());
        return retryHistory.size();
    }
    
    /**
     * Retourne le délai d'attente avant la prochaine tentative
     */
    public Duration getBackoffDelay() {
        return backoffDelay;
    }
    
    /**
     * Calcule le délai avec backoff exponentiel
     */
    public Duration getExponentialBackoff() {
        int attempts = getCurrentRetryCount();
        long delayMillis = (long) (backoffDelay.toMillis() * Math.pow(2, attempts));
        return Duration.ofMillis(Math.min(delayMillis, Duration.ofMinutes(5).toMillis()));
    }
    
    /**
     * Nettoie les tentatives trop anciennes
     */
    private void cleanOldRetries(LocalDateTime now) {
        retryHistory.removeIf(retryTime -> {
            Duration age = Duration.between(retryTime, now);
            return age.compareTo(withinDuration) > 0;
        });
    }
    
    /**
     * Retourne des statistiques
     */
    public RetryStats getStats() {
        return new RetryStats(
            getCurrentRetryCount(),
            maxRetries,
            withinDuration,
            backoffDelay
        );
    }
    
    @Override
    public String toString() {
        return String.format("RetryPolicy[%d/%d tentatives dans %s, backoff=%s]",
                           getCurrentRetryCount(), maxRetries, 
                           withinDuration, backoffDelay);
    }
    
    /**
     * Statistiques de retry
     */
    public static class RetryStats {
        public final int currentRetries;
        public final int maxRetries;
        public final Duration window;
        public final Duration backoff;
        
        public RetryStats(int currentRetries, int maxRetries, 
                         Duration window, Duration backoff) {
            this.currentRetries = currentRetries;
            this.maxRetries = maxRetries;
            this.window = window;
            this.backoff = backoff;
        }
        
        public boolean hasReachedLimit() {
            return currentRetries >= maxRetries;
        }
        
        public int getRemainingRetries() {
            return Math.max(0, maxRetries - currentRetries);
        }
        
        @Override
        public String toString() {
            return String.format("Retries: %d/%d (remaining: %d) in %s, backoff: %s",
                               currentRetries, maxRetries, 
                               getRemainingRetries(), window, backoff);
        }
    }
}