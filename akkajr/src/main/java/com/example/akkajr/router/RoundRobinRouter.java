package com.example.akkajr.router;

import com.example.akkajr.core.actors.ActorRef;
import java.util.List;

public class RoundRobinRouter extends AbstractRouter{
	
	private int index = 0;
	
	public RoundRobinRouter(List<ActorRef> routes) {
		super(routes);
	}
	
	@Override
	protected ActorRef selectRoute() {
		if (routes.isEmpty()) return null;
		
		ActorRef ref = routes.get(index);
		index = (index + 1) % routes.size();
		return ref;
	}
	

}
