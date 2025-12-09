package com.example.akkajr.core.actors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ActorSystemTests {

    private static final Logger LOG = Logger.getLogger(ActorSystemTests.class.getName());

    private ActorSystem system;

    @AfterEach
    void tearDown() {
        if (system != null) {
            system.shutdown();
        }
    }

    @Test
    void actorReceivesMessagesViaMailbox() throws Exception {
        system = new ActorSystem();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> sink = new AtomicReference<>();

        LOG.info("[MAILBOX] create /user/probe");
        ActorRef ref = system.actorOf(Props.create(ProbeActor.class, latch, sink), "probe");

        LOG.info("[MAILBOX] send 'hello'");
        ref.tell("hello", null);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Message was not processed in time");
        assertEquals("hello", sink.get());

        system.stop(ref);
        LOG.info("Stopped /user/probe");
    }

    @Test
    void lifecycleHooksAreCalledOnStartAndStop() {
        system = new ActorSystem();
        LOG.info("[LIFECYCLE] create /user/life");
        AtomicInteger preStartCalls = new AtomicInteger();
        AtomicInteger postStopCalls = new AtomicInteger();

        ActorRef ref = system.actorOf(Props.create(LifecycleProbeActor.class, preStartCalls, postStopCalls), "life");

        assertEquals(1, preStartCalls.get(), "preStart should be called once on start");
        system.stop(ref);
        assertEquals(1, postStopCalls.get(), "postStop should be called once on stop");
        LOG.info("[LIFECYCLE] preStart=" + preStartCalls.get() + " postStop=" + postStopCalls.get());
    }

    @Test
    void messagesAreProcessedInFifoOrder() throws Exception {
        system = new ActorSystem();
        LOG.info("[FIFO] create /user/fifo");
        CountDownLatch latch = new CountDownLatch(3);
        List<String> received = new ArrayList<>();

        ActorRef ref = system.actorOf(Props.create(FifoProbeActor.class, latch, received), "fifo");

        LOG.info("[FIFO] send a, b, c");
        ref.tell("a", null);
        ref.tell("b", null);
        ref.tell("c", null);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Messages were not processed in time");
        assertEquals(List.of("a", "b", "c"), received, "Messages should keep FIFO order");

        system.stop(ref);
        LOG.info("[FIFO] received order=" + received);
    }

    @Test
    void childActorGetsHierarchicalPathAndParent() throws Exception {
        system = new ActorSystem();
        LOG.info("[HIERARCHY] create parent /user/parent");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> childPathSeen = new AtomicReference<>();
        AtomicReference<String> parentPathSeen = new AtomicReference<>();

        ActorRef parent = system.actorOf(Props.create(ParentProbeActor.class, childPathSeen, parentPathSeen, latch), "parent");

        LOG.info("[HIERARCHY] spawn child /user/parent/child");
        parent.tell("spawn-child", null);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Child path not observed in time");
        assertEquals("/user/parent/child", childPathSeen.get(), "Child path should be hierarchical");
        assertEquals("/user/parent", parentPathSeen.get(), "Child should see its parent path");

        system.stop(parent);
    }

    @Test
    void creatingChildWithNullParentThrows() {
        system = new ActorSystem();
        LOG.info("[HIERARCHY-NEG] attempt child with null parent");
        assertThrows(IllegalArgumentException.class, () -> system.actorOfChild(Props.create(EchoActor.class), "child", null));
    }

    @Test
    void creatingChildUnderStoppedParentFails() {
        system = new ActorSystem();
        LOG.info("[HIERARCHY-NEG] parent stopped before child creation");
        ActorRef parent = system.actorOf(Props.create(EchoActor.class), "parent-stop");
        system.stop(parent);
        assertThrows(IllegalArgumentException.class, () -> system.actorOfChild(Props.create(EchoActor.class), "child", parent));
    }

    @Test
    void duplicateChildNameUnderSameParentFails() {
        system = new ActorSystem();
        LOG.info("[HIERARCHY-NEG] duplicate child name under same parent");
        ActorRef parent = system.actorOf(Props.create(EchoActor.class), "parent-dup");
        ActorRef firstChild = system.actorOfChild(Props.create(EchoActor.class), "child", parent);
        assertEquals("/user/parent-dup/child", firstChild.path().value());
        assertThrows(IllegalArgumentException.class, () -> system.actorOfChild(Props.create(EchoActor.class), "child", parent));
        system.stop(parent);
    }

    // Simple probe actor for testing message delivery
    public static class ProbeActor extends Actor {
        private final CountDownLatch latch;
        private final AtomicReference<Object> sink;

        public ProbeActor(CountDownLatch latch, AtomicReference<Object> sink) {
            this.latch = latch;
            this.sink = sink;
        }

        @Override
        public void receive(Object message, ActorRef sender) {
            sink.set(message);
            latch.countDown();
        }
    }

    public static class LifecycleProbeActor extends Actor {
        private final AtomicInteger preStartCalls;
        private final AtomicInteger postStopCalls;

        public LifecycleProbeActor(AtomicInteger preStartCalls, AtomicInteger postStopCalls) {
            this.preStartCalls = preStartCalls;
            this.postStopCalls = postStopCalls;
        }

        @Override
        public void preStart() {
            preStartCalls.incrementAndGet();
        }

        @Override
        public void receive(Object message, ActorRef sender) {
            // no-op
        }

        @Override
        public void postStop() {
            postStopCalls.incrementAndGet();
        }
    }

    public static class FifoProbeActor extends Actor {
        private final CountDownLatch latch;
        private final List<String> received;

        public FifoProbeActor(CountDownLatch latch, List<String> received) {
            this.latch = latch;
            this.received = received;
        }

        @Override
        public void receive(Object message, ActorRef sender) {
            received.add(String.valueOf(message));
            latch.countDown();
        }
    }

    public static class ParentProbeActor extends Actor {
        private final AtomicReference<String> childPathSeen;
        private final AtomicReference<String> parentPathSeen;
        private final CountDownLatch latch;

        public ParentProbeActor(AtomicReference<String> childPathSeen, AtomicReference<String> parentPathSeen, CountDownLatch latch) {
            this.childPathSeen = childPathSeen;
            this.parentPathSeen = parentPathSeen;
            this.latch = latch;
        }

        @Override
        public void receive(Object message, ActorRef sender) {
            if ("spawn-child".equals(message)) {
                ActorRef child = getContext().actorOf(Props.create(ChildProbeActor.class, parentPathSeen, latch), "child");
                childPathSeen.set(child.path().value());
                // Ask the child to report its parent so it runs receive and captures parent path
                child.tell("who-is-your-parent", getContext().getSelf());
            }
        }
    }

    public static class ChildProbeActor extends Actor {
        private final AtomicReference<String> parentPathSeen;
        private final CountDownLatch latch;

        public ChildProbeActor(AtomicReference<String> parentPathSeen, CountDownLatch latch) {
            this.parentPathSeen = parentPathSeen;
            this.latch = latch;
        }

        @Override
        public void receive(Object message, ActorRef sender) {
            if ("who-is-your-parent".equals(message) && getContext().getParent() != null) {
                parentPathSeen.set(getContext().getParent().path().value());
                latch.countDown();
            }
        }
    }
}
