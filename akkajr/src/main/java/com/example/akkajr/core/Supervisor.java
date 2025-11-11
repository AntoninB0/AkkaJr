package com.example.akkajr.core;

/**
 * Interface définissant le comportement d'un superviseur
 * Inspiré du modèle de supervision d'Akka
 */
public interface Supervisor {
    
    /**
     * Mode de supervision : définit comment les échecs se propagent
     */
    enum SupervisionMode {
        ONE_FOR_ONE,   // Seul le service en échec est affecté
        ALL_FOR_ONE    // Tous les services sont affectés
    }
    
    /**
     * Gère l'échec d'un service
     */
    void handleFailure(Service failedService, Throwable cause);
    
    /**
     * Retourne le mode de supervision actuel
     */
    SupervisionMode getSupervisionMode();
    
    /**
     * Définit le mode de supervision
     */
    void setSupervisionMode(SupervisionMode mode);
}