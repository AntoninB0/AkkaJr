package com.example.akkajr.router;

//import com.example.akkajr.core.actors.Actor;
//import com.example.akkajr.core.actors.ActorRef;

import com.example.akkajr.messaging.Message;
import com.example.akkajr.messaging.MessageService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public abstract class AbstractRouter { //class centrale
	
	//injection du service de messagerie
	@Autowired
    protected MessageService messageService;
	
	// Liste des identifiants des workers vers lesquels router (ex: "workerA", "workerB")
    protected List<String> routeIds;
	
	public AbstractRouter(List<String> routeIds) {
		this.routeIds = routeIds;
	}
	
	public void route(Message message) {
        // 1. Choisir la destination selon la stratégie (Round Robin, Broadcast, etc.)
        String targetId = selectRoute();

        if (targetId != null) {
            // 2. Mettre à jour le destinataire du message
            message.setReceiverId(targetId);
            
            // 3. Réinjecter le message dans le service pour l'envoi final
            // Cela permet de profiter de l'historique, des dead letters et du remote.
            messageService.send(message);
        } else {
            System.err.println("[ROUTER] Erreur : Aucune destination disponible pour le message " + message);
        }
    }
	
	
	/**
     * Logique de sélection spécifique à chaque type de routeur.
     * @return L'ID de l'agent destinataire (String).
     */
    protected abstract String selectRoute();
    
    // Getter et Setter pour permettre de modifier les workers dynamiquement (Bloc 7)
    public List<String> getRouteIds() {
        return routeIds;
    }

    public void setRouteIds(List<String> routeIds) {
        this.routeIds = routeIds;
    }
	

}
