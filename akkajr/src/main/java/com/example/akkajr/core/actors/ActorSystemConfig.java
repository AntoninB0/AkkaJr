package com.example.akkajr.core.actors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class ActorSystemConfig {
    
    @Bean
    public ActorSystem actorSystem(MeterRegistry registry) {
        return new ActorSystem(registry);
    }
}