package com.example.akkajr;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import com.typesafe.config.ConfigFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
public class AkkajrApplication {

    public static void main(String[] args) {
        SpringApplication.run(AkkajrApplication.class, args);
    }

    @Bean
    @Profile("!test")
    public ActorSystem<Void> actorSystem(
            @Value("${akka.port:2551}") int akkaPort) {
        var config = ConfigFactory.load();
        if (akkaPort != 2551) {
            var portConfig = ConfigFactory.parseString(
                "akka.remote.artery.canonical.port=" + akkaPort
            );
            config = portConfig.withFallback(config);
        }
        // Mode cluster nominal
        return ActorSystem.create(Behaviors.empty(), "ClusterSystem", config);
    }

    @Bean
    @Profile("test")
    public ActorSystem<Void> actorSystemTest() {
        var testCfg = ConfigFactory.parseString(
            "akka.actor.provider = local\n" +
            "akka.remote.artery.canonical.port = 0\n"
        );
        var config = testCfg.withFallback(ConfigFactory.load());
        return ActorSystem.create(Behaviors.empty(), "TestSystem", config);
    }
}