package com.example.akkajr.cluster;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;

public class ClusterListener extends AbstractActor {

    private final Cluster cluster = Cluster.get(getContext().getSystem());

    @Override
    public void preStart() {
        cluster.subscribe(
            getSelf(),
            ClusterEvent.initialStateAsEvents(),
            ClusterEvent.MemberUp.class,
            ClusterEvent.MemberRemoved.class
        );
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(ClusterEvent.MemberUp.class,
                e -> System.out.println("NODE UP : " + e.member()))
            .match(ClusterEvent.MemberRemoved.class,
                e -> System.out.println("NODE DOWN : " + e.member()))
            .build();
    }
}
