package com.example.akkajr.sharding;

import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import com.example.akkajr.actors.ServiceActor;
import com.example.akkajr.messages.ServiceCommand;

public class ServiceSharding {

    public static final EntityTypeKey<ServiceCommand> SERVICE_KEY =
        EntityTypeKey.create(ServiceCommand.class, "Service");

    public static void init(ActorSystem<Void> system) {
        ClusterSharding sharding = ClusterSharding.get(system);
        
        sharding.init(
            Entity.of(
                SERVICE_KEY,
                entityContext -> ServiceActor.create(entityContext.getEntityId())
            )
        );
    }
}