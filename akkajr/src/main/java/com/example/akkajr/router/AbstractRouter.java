package com.example.akkajr.router;

import java.util.List;

import com.example.akkajr.messaging.Message;
import com.example.akkajr.messaging.MessageService;

public abstract class AbstractRouter {
    
    // ❌ RETIRER @Autowired (injecté manuellement par le controller)
    public MessageService messageService; // package-private ou public
    
    protected List<String> routeIds;
    
    public AbstractRouter(List<String> routeIds) {
        this.routeIds = routeIds;
    }
    
    public void route(Message message) {
        String targetId = selectRoute();
        if (targetId != null) {
            message.setReceiverId(targetId);
            messageService.send(message);
        } else {
            System.err.println("[ROUTER] Erreur : Aucune destination disponible");
        }
    }
    
    protected abstract String selectRoute();
}