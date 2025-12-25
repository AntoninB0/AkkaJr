package com.example.akkajr.router;

import com.example.akkajr.messaging.Message;
import com.example.akkajr.messaging.MessageService;
import java.util.Arrays;

public class RouterMessagingTest {
    public static void main(String[] args) {
        // 1. Simuler le service de messagerie (le Bloc 2)
        // Note : On utilise un "faux" service pour le test si Spring n'est pas lancé
        MessageService mockService = new MessageService() {
            @Override
            public void send(Message msg) {
                System.out.println("[MOCK SEND] Message envoyé à : " + msg.getReceiverId() 
                                   + " | Contenu : " + msg.getContent());
            }
        };

        // 2. Créer ton RoundRobinRouter avec des IDs de workers (Bloc 7)
        RoundRobinRouter rrRouter = new RoundRobinRouter(Arrays.asList("worker-1", "worker-2", "worker-3"));
        
        // On "injecte" manuellement le service (puisqu'on n'est pas dans Spring ici)
        rrRouter.messageService = mockService; 

        // 3. Envoyer plusieurs messages au routeur pour voir la répartition
        System.out.println("--- Test Round Robin ---");
        for (int i = 1; i <= 4; i++) {
            Message msg = new Message("sender-test", "router-id", "Tâche " + i);
            rrRouter.route(msg);
        }

        // 4. Tester le Broadcast
        System.out.println("\n--- Test Broadcast ---");
        BroadcastRouter bRouter = new BroadcastRouter(Arrays.asList("worker-A", "worker-B"));
        bRouter.messageService = mockService;
        
        Message alert = new Message("admin", "broadcast-id", "Alerte Système");
        bRouter.route(alert);
    }
}
