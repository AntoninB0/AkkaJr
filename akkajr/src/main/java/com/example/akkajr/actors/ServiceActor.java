package com.example.akkajr.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.example.akkajr.messages.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ServiceActor extends AbstractBehavior<ServiceCommand> {
    
    private final String serviceId;
    private final List<String> commands;
    private Instant lastHeartbeat;
    private boolean isRunning;
    
    public static Behavior<ServiceCommand> create(String serviceId) {
        return Behaviors.setup(context -> new ServiceActor(context, serviceId));
    }
    
    private ServiceActor(ActorContext<ServiceCommand> context, String serviceId) {
        super(context);
        this.serviceId = serviceId;
        this.commands = new ArrayList<>();
        this.lastHeartbeat = Instant.now();
        this.isRunning = false;
        
        getContext().getLog().info("Service actor {} créé à {}", serviceId, lastHeartbeat);
    }
    
    @Override
    public Receive<ServiceCommand> createReceive() {
        return newReceiveBuilder()
            .onMessage(Heartbeat.class, this::onHeartbeat)
            .onMessage(StartService.class, this::onStartService)
            .onMessage(StopService.class, this::onStopService)
            .onMessage(AddCommand.class, this::onAddCommand)
            // Optionnel : ajouter un message de statut pour lire les variables
            // .onMessage(GetStatus.class, this::onGetStatus) 
            .build();
    }
    
    private Behavior<ServiceCommand> onHeartbeat(Heartbeat msg) {
        this.lastHeartbeat = Instant.now();
        // CORRECTION : On utilise la variable dans le log pour qu'elle soit considérée comme "utilisée"
        getContext().getLog().debug("Heartbeat reçu pour {}. Dernier check: {}", serviceId, lastHeartbeat);
        return this;
    }
    
    private Behavior<ServiceCommand> onStartService(StartService msg) {
        this.isRunning = true;
        // CORRECTION : On utilise isRunning dans le log
        getContext().getLog().info("Service {} démarré. État actuel : running={}", serviceId, isRunning);
        msg.getReplyTo().tell(new ServiceStarted(serviceId));
        return this;
    }
    
    private Behavior<ServiceCommand> onStopService(StopService msg) {
        this.isRunning = false;
        getContext().getLog().info("Arrêt service {}. État final : running={}", serviceId, isRunning);
        msg.getReplyTo().tell(new ServiceStopped(serviceId));
        return Behaviors.stopped();
    }
    
    private Behavior<ServiceCommand> onAddCommand(AddCommand msg) {
        // CORRECTION : On vérifie si le service tourne avant d'ajouter une commande
        if (isRunning) {
            getContext().getLog().info("Commande '{}' ajoutée à {}", msg.getCommand(), serviceId);
            commands.add(msg.getCommand());
            msg.getReplyTo().tell(new CommandAdded(serviceId, commands.size()));
        } else {
            getContext().getLog().warn("Impossible d'ajouter la commande : le service {} est arrêté", serviceId);
        }
        return this;
    }
}