package com.example.akkajr;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import com.example.akkajr.sharding.ServiceSharding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.annotation.PreDestroy;

@SpringBootApplication
public class AkkaClusterApplication {
    
    private ActorSystem<Void> actorSystem;
    
    public static void main(String[] args) {
        SpringApplication.run(AkkaClusterApplication.class, args);
    }
    
    @Bean
    public ActorSystem<Void> actorSystem() {
        actorSystem = ActorSystem.create(
            Behaviors.empty(), 
            "ClusterSystem"
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