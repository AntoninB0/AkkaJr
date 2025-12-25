package com.example.akkajr.router;

import com.example.akkajr.core.actors.Actor;
import com.example.akkajr.core.actors.ActorRef;

import java.util.List;

public class BroadcastRouter extends AbstractRouter{
	
	public BroadcastRouter(List<ActorRef> routes) {
		super(routes);
	}
	
	
	@Override
    protected ActorRef selectRoute() {
        return null; // inutile ici, car on redéfinit receive plus bas
    }
	
	@Override
	public void receive(Object message, ActorRef sender) throws Exception {
		for (ActorRef ref : routes) {
			ref.tell(message,  sender);
		}
	}
	

}
