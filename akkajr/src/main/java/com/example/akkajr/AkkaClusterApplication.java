package com.example.akkajr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.akkajr.sharding.ServiceSharding;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
// Changement d'import : javax -> jakarta
import jakarta.annotation.PreDestroy;

@SpringBootApplication
public class AkkaClusterApplication {
    
    private ActorSystem<Void> actorSystem;
    
    public static void main(String[] args) {
        SpringApplication.run(AkkaClusterApplication.class, args);
    }
    
    @Bean
    public ActorSystem<Void> actorSystem() {
        // Le nom "ClusterSystem" doit correspondre à celui dans votre application.conf
        actorSystem = ActorSystem.create(
            Behaviors.empty(), 
            "AkkajrApplication" 
        );
        
        // Initialiser le sharding
        ServiceSharding.init(actorSystem);
        
        return actorSystem;
    }
    
    @PreDestroy
    public void shutdown() {
        if (actorSystem != null) {
            actorSystem.terminate();
        }
    }
}