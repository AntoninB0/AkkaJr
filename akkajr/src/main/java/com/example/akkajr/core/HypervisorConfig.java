package com.example.akkajr.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HypervisorConfig {
    
    @Value("${hypervisor.healthcheck.interval:10}")
    private int healthCheckInterval;
    
    @Bean
    public Hypervisor hypervisor() {
        Hypervisor hypervisor = new Hypervisor(healthCheckInterval);
        hypervisor.start();
        return hypervisor;
    }
}