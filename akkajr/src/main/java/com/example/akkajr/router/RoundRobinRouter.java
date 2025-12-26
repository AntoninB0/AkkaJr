package com.example.akkajr.router;

import java.util.List;


/**
 * Implémente la stratégie Round Robin : distribue les messages un par un
 * à chaque worker de la liste de manière circulaire.
 */

public class RoundRobinRouter extends AbstractRouter{
	
	private int index = 0;
	
	public RoundRobinRouter(List<String> routeIds) {
		super(routeIds);
	}
	
	@Override
	protected String selectRoute() {
		if (routeIds == null || routeIds.isEmpty()) {
			return null;
		}
		
		// Sélection de l'ID actuel
        String targetId = routeIds.get(index);
        
		index = (index + 1) % routeIds.size();
		return targetId;
	}
	

}
