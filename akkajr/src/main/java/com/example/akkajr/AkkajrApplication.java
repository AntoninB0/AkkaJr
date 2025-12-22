package com.example.akkajr;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import com.typesafe.config.ConfigFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AkkajrApplication {

    public static void main(String[] args) {
        SpringApplication.run(AkkajrApplication.class, args);
    }

    @Bean
    public ActorSystem<Void> actorSystem() {
        // Force le chargement explicite de application.conf
        return ActorSystem.create(Behaviors.empty(), "AkkajrApplication", ConfigFactory.load());
    }
}