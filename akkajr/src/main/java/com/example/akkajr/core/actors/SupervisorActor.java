package com.example.akkajr.core.actors;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SupervisorActor extends Actor {
    
    private final Map<String, ActorRef> children = new ConcurrentHashMap<>();
    private final Map<String, Integer> restartCounts = new ConcurrentHashMap<>();
    private static final int MAX_RESTARTS = 3;
    
    @Override
    public void preStart() {
        logger.info("Supervisor démarré avec stratégie de supervision");
    }
    
    @Override
    public void receive(Object message, ActorRef sender) {
        if (message instanceof CreateChildRequest req) {
            handleCreateChild(req, sender);
        } else if (message instanceof RouteMessage route) {
            handleRouteMessage(route, sender);
        } else if (message instanceof GetChildrenRequest) {
            sender.tell(children.keySet(), getContext().getSelf());
        } else if (message instanceof GetChildRef get) {
            ActorRef child = children.get(get.name);
            sender.tell(child, getContext().getSelf());
        } else if (message instanceof RestartChild restart) {
            handleRestartChild(restart, sender);
        } else if (message instanceof SupervisionStrategy strategy) {
            handleSupervisionStrategy(strategy, sender);
        }
    }
    
    private void handleCreateChild(CreateChildRequest req, ActorRef sender) {
        try {
            ActorRef child = getContext().actorOf(req.props, req.name);
            children.put(req.name, child);
            restartCounts.put(req.name, 0);
            logger.info("Enfant créé: " + req.name + " (" + child.path() + ")");
            sender.tell(new CreateChildResponse(true, child.path().value()), getContext().getSelf());
        } catch (Exception e) {
            logger.severe("Erreur création: " + e.getMessage());
            sender.tell(new CreateChildResponse(false, e.getMessage()), getContext().getSelf());
        }
    }
    
    private void handleRouteMessage(RouteMessage route, ActorRef sender) {
        ActorRef target = children.get(route.targetName);
        if (target != null) {
            try {
                target.tell(route.message, sender);
            } catch (Exception e) {
                logger.warning("Erreur lors du routing vers " + route.targetName + ": " + e.getMessage());
                // Tenter un redémarrage
                handleChildFailure(route.targetName, e);
            }
        } else {
            sender.tell(new ErrorResponse("Service non trouvé: " + route.targetName), getContext().getSelf());
        }
    }
    
    private void handleRestartChild(RestartChild restart, ActorRef sender) {
        String childName = restart.childName;
        int restarts = restartCounts.getOrDefault(childName, 0);
        
        if (restarts >= MAX_RESTARTS) {
            logger.severe("Limite de redémarrages atteinte pour " + childName + " - arrêt définitif");
            ActorRef child = children.remove(childName);
            if (child != null) {
                getContext().stop(child);
            }
            sender.tell(new RestartResponse(false, "Max restarts exceeded"), getContext().getSelf());
            return;
        }
        
        logger.info("Redémarrage de " + childName + " (tentative " + (restarts + 1) + "/" + MAX_RESTARTS + ")");
        
        ActorRef oldChild = children.get(childName);
        if (oldChild != null) {
            getContext().stop(oldChild);
        }
        
        // Recréer l'acteur (nécessite de stocker les Props)
        restartCounts.put(childName, restarts + 1);
        sender.tell(new RestartResponse(true, "Child restarted"), getContext().getSelf());
    }
    
    private void handleChildFailure(String childName, Exception error) {
        logger.warning("Échec détecté pour " + childName + ": " + error.getMessage());
        // Auto-redémarrage
        handleRestartChild(new RestartChild(childName), getContext().getSelf());
    }
    
    private void handleSupervisionStrategy(SupervisionStrategy strategy, ActorRef sender) {
        logger.info("Application de la stratégie: " + strategy.strategy);
        // Implémenter One-For-One, All-For-One, etc.
        sender.tell(new StrategyApplied(strategy.strategy), getContext().getSelf());
    }
    
    // Messages
    public record CreateChildRequest(Props props, String name) {}
    public record CreateChildResponse(boolean success, String info) {}
    public record RouteMessage(String targetName, Object message) {}
    public record GetChildrenRequest() {}
    public record GetChildRef(String name) {} 
    public record ErrorResponse(String error) {}
    public record RestartChild(String childName) {}
    public record RestartResponse(boolean success, String message) {}
    public record SupervisionStrategy(String strategy) {} // "one-for-one", "all-for-one"
    public record StrategyApplied(String strategy) {}
}