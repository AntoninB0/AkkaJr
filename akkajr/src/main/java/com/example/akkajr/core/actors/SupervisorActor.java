package com.example.akkajr.core.actors;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SupervisorActor extends Actor {
    
    private final Map<String, ActorRef> children = new ConcurrentHashMap<>();
    
    @Override
    public void preStart() {
        logger.info("Supervisor démarré");
    }
    
    @Override
    public void receive(Object message, ActorRef sender) {
        if (message instanceof CreateChildRequest req) {
            handleCreateChild(req, sender);
        } else if (message instanceof RouteMessage route) {
            handleRouteMessage(route, sender);
        } else if (message instanceof GetChildrenRequest) {
            sender.tell(children.keySet(), getContext().getSelf());
        }
        else if (message instanceof GetChildRef get) {
            ActorRef child = children.get(get.name);
            sender.tell(child, getContext().getSelf());
        }
    }
    
    private void handleCreateChild(CreateChildRequest req, ActorRef sender) {
        try {
            ActorRef child = getContext().actorOf(req.props, req.name);
            children.put(req.name, child);
            logger.info("Enfant créé: " + req.name);
            sender.tell(new CreateChildResponse(true, child.path().value()), getContext().getSelf());
        } catch (Exception e) {
            logger.severe("Erreur création: " + e.getMessage());
            sender.tell(new CreateChildResponse(false, e.getMessage()), getContext().getSelf());
        }
    }
    
    private void handleRouteMessage(RouteMessage route, ActorRef sender) {
        ActorRef target = children.get(route.targetName);
        if (target != null) {
            target.tell(route.message, sender);
        } else {
            sender.tell(new ErrorResponse("Service non trouvé: " + route.targetName), getContext().getSelf());
        }
    }
    
    // Messages
    public record CreateChildRequest(Props props, String name) {}
    public record CreateChildResponse(boolean success, String info) {}
    public record RouteMessage(String targetName, Object message) {}
    public record GetChildrenRequest() {}
    public record GetChildRef(String name) {} 
    public record ErrorResponse(String error) {}
}