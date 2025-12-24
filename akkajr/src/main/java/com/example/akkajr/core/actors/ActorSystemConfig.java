package com.example.akkajr.core.actors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class ActorSystemConfig {
    
    @Bean(name = "coreActorSystem")
    public ActorSystem coreActorSystem(ObjectProvider<MeterRegistry> registryProvider) {
        // Allow running without Micrometer registry (tests) while wiring metrics when present
        return new ActorSystem(registryProvider.getIfAvailable());
    }
}