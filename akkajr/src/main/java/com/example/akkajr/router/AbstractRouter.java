package com.example.akkajr.router;

import com.example.akkajr.core.actors.Actor;
import com.example.akkajr.core.actors.ActorRef;

import java.util.List;

public abstract class AbstractRouter extends Actor{
	protected List<ActorRef> routes;
	
	public AbstractRouter(List<ActorRef> routes) {
		this.routes = routes;
	}
	
	@Override
	public void receive(Object message, ActorRef sender) throws Exception {
        ActorRef target = selectRoute();
        if (target != null) {
            target.tell(message, sender);
        }
    }
	
	protected abstract ActorRef selectRoute();

}
