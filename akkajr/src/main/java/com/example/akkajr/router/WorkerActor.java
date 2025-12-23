package com.example.akkajr.router;

import com.example.akkajr.core.actors.Actor;
import com.example.akkajr.core.actors.ActorRef;

public class WorkerActor extends Actor{	//cette classe permet de voir qui reçoit quoi (utile pour les tests)
	private final String name;
	
	public WorkerActor(String name) {
		this.name = name;
	}
	
	@Override
    public void receive(Object message, ActorRef sender) {
        System.out.println(
            "Worker " + name + " a reçu : " + message
        );
    }

}
