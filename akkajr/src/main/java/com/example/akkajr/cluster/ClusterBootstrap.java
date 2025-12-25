package com.example.akkajr.cluster;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.typed.javadsl.Adapter; // Import crucial
import com.example.akkajr.sharding.ServiceSharding;

public class ClusterBootstrap {

    public static void init(ActorSystem system) {
        // Crée le listener dans le monde "Classic"
        system.actorOf(Props.create(ClusterListener.class), "clusterListener");
        
        // CORRECTION : Conversion du système Classic vers Typed pour le Sharding
        akka.actor.typed.ActorSystem<Void> typedSystem = Adapter.toTyped(system);
        
        // On passe maintenant le système typé attendu par ServiceSharding
        ServiceSharding.init(typedSystem);
    }
}