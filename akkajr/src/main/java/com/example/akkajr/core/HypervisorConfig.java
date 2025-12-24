package com.example.akkajr.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

/**
 * Configuration Spring pour l'Hypervisor
 */
@Configuration
public class HypervisorConfig {
    
    @Value("${hypervisor.healthcheck.interval:10000}")
    private long healthCheckInterval;
    
    private Hypervisor hypervisor;
    
    /**
     * Crée et démarre l'Hypervisor
     * @return Instance de l'Hypervisor
     */
    @Bean
    public Hypervisor hypervisor() {
        hypervisor = new Hypervisor(healthCheckInterval);
        hypervisor.start();
        return hypervisor;
    }
    
    /**
     * Arrête l'Hypervisor au shutdown de l'application
     */
    @PreDestroy
    public void shutdown() {
        if (hypervisor != null && hypervisor.isRunning()) {
            hypervisor.stop();
        }
    }
}