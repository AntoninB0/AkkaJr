package com.example.akkajr.router;

import com.example.akkajr.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implémente la stratégie Broadcast : envoie une copie du message
 * à TOUS les workers configurés.
 */
@Component("broadcastRouter")
public class BroadcastRouter extends AbstractRouter{
	
	public BroadcastRouter(List<String> routeIds) {
		super(routeIds);
	}
	
	
	@Override
	public void route(Message message) {
        if (routeIds == null || routeIds.isEmpty()) return;

        System.out.println("[ROUTER] Broadcast vers " + routeIds.size() + " workers.");
        
        for (String targetId : routeIds) {
            // On crée une copie du message pour chaque destinataire
            Message copy = new Message(
                message.getSenderId(), 
                targetId, 
                message.getContent()
            );
            // On utilise le service pour l'envoi
            messageService.send(copy);
        }
    }
	

}
