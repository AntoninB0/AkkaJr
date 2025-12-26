# AkkaJr

# Install

Build

```sh
cd ./akkajr/
docker build -t akkajr .
```

run

```sh
cd ./akkajr/
docker run -p 8080:8080 akkajr
localhost:8080
```


# Lancer les tests 
```sh
cd ./akkajr/
 PowerShell -ExecutionPolicy Bypass -File .\test-complet-simple.ps1
 ```


# Vs Code extension

https://marketplace.visualstudio.com/items?itemName=Al-rimi.tomcat
https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-test
https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-spring-initializr
https://marketplace.visualstudio.com/items?itemName=vmware.vscode-spring-boot
https://marketplace.visualstudio.com/items?itemName=vmware.vscode-boot-dev-pack
https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-spring-boot-dashboard
https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-dependency
https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-maven
https://marketplace.visualstudio.com/items?itemName=redhat.java
https://marketplace.visualstudio.com/items?itemName=Oracle.oracle-java
https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-gradle
https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack
https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-debug
https://marketplace.visualstudio.com/items?itemName=ms-azuretools.vscode-containers

bash# Arrêter tous les conteneurs
docker stop $(docker ps -q)

# Supprimer les conteneurs arrêtés

docker container prune -f

# Puis relancer

docker run -p 8080:8080 akkajr


# Commandes utiles (local)
```sh
# Tests unitaires
cd akkajr
./mvnw test

# Lancer en mode test (provider local, pas de cluster)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=test --server.port=8080"

# Lancer 2 nœuds (cluster) si besoin
# Nœud 1 (seed 2551)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8080 --app.service.name=service1 --app.remote.services=service2=http://localhost:8081 --akka.port=2551"
# Nœud 2 (2552)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081 --app.service.name=service2 --app.remote.services=service1=http://localhost:8080 --akka.port=2552"


# Exemple messaging (intra service1)
curl -X POST http://localhost:8080/api/messages/tell \
  -H "Content-Type: application/json" \
  -d '{"senderId":"actor1","receiverId":"actor2","content":"Hello"}'
curl -X POST http://localhost:8080/api/messages/ask \
  -H "Content-Type: application/json" \
  -d '{"senderId":"actor1","receiverId":"actor2","content":"Status?"}'
curl http://localhost:8080/api/messages/inbox/actor2
curl http://localhost:8080/api/messages/history
curl http://localhost:8080/api/messages/deadletters
curl http://localhost:8080/api/messages/stats

# Init acteurs démo interne (order/payment/notification)
curl -X POST http://localhost:8080/api/actors/init
curl -X POST http://localhost:8080/api/actors/order \
  -H "Content-Type: application/json" \
  -d '{"items":["Phone","Tablet"]}'
```

# Monitoring & Observabilité

- Endpoints principaux
	- UI live: http://localhost:8080/observability (SSE metrics/alerts, tableau par acteur, health, feed d'événements)
	- Snapshot agrégé: GET /api/metrics/actors
	- Détail acteurs: GET /api/metrics/actors/detail (backlog, processed, failed, paused, guardian)
	- Alertes: GET /api/metrics/alerts
	- Stream SSE: GET /api/metrics/stream
	- Événements récents: GET /api/metrics/events (msgId/traceId, processed/failed)
	- Health: GET /actuator/health (porte OUT_OF_SERVICE si backlog élevé)
	- Prometheus: GET /actuator/prometheus (toutes les métriques Micrometer, y compris per-actor)

- Lancer l'app localement
	```sh
	cd akkajr
	./mvnw spring-boot:run
	# Ouvrir /observability pour le dashboard
	```

- Ce qui est instrumenté
	- Compteurs: acteurs créés/stoppés, messages processed/failed par acteur, backlog et paused agrégés.
	- Latence: timer Micrometer par acteur (exposé dans /actuator/prometheus).
	- Health: backlog élevé => OUT_OF_SERVICE via ActorSystemHealthIndicator.
	- Traçabilité: msgId/traceId dans logs et dans /api/metrics/events (feed UI).
	- Alertes: backlog/paused/messages_failed surfacent dans SSE et UI.
