package com.example.akkajr.router;

import com.example.akkajr.core.actors.ActorRef;
import com.example.akkajr.core.actors.ActorSystem;
import com.example.akkajr.router.RoundRobinRouter;
import com.example.akkajr.router.BroadcastRouter;
import com.example.akkajr.core.actors.Props;

import java.util.ArrayList;
import java.util.List;


public class RouterTest {
	public static void main(String[] args) {
		ActorSystem system = new ActorSystem();
		
		List<ActorRef> workers = new ArrayList<>();
		workers.add(system.actorOf(Props.create(WorkerActor.class, "A"), "workerA"));
		workers.add(system.actorOf(Props.create(WorkerActor.class, "B"), "workerB"));
		workers.add(system.actorOf(Props.create(WorkerActor.class, "C"), "workerC"));
		
		ActorRef router = system.actorOf(Props.create(RoundRobinRouter.class, workers), "router"); //pour tester selon strategie de RoundRobinRouter 
		
		ActorRef router2 = system.actorOf(Props.create(BroadcastRouter.class, workers), "router2"); //pour tester selon strategie de BroadcastRouter
		
		router.tell("Message 1", null);
        router.tell("Message 2", null);
        router.tell("Message 3", null);
        router.tell("Message 4", null);
        router.tell("Message 5", null);
                
        router2.tell("Message 1", null);
        router2.tell("Message 2", null);
        router2.tell("Message 3", null);
        router2.tell("Message 4", null);
        router2.tell("Message 5", null);
	}

}
