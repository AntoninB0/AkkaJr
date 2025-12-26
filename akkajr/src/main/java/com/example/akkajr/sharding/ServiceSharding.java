package com.example.akkajr.sharding;

import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;

public class ServiceSharding {

    // Utilisation de Object pour éviter les erreurs sur des classes manquantes
    public static final EntityTypeKey<Object> ENTITY_TYPE_KEY = 
        EntityTypeKey.create(Object.class, "ServiceEntity");

    public static void init(ActorSystem<Void> system) {
        ClusterSharding sharding = ClusterSharding.get(system);
        System.out.println("--- Akka Cluster Sharding initialisé ---");
    }
}