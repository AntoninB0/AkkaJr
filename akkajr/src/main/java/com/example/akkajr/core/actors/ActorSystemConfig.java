package com.example.akkajr.core.actors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActorSystemConfig {
    
    @Bean
    public ActorSystem actorSystem() {
        return new ActorSystem();
    }
}