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



# tests automatisés

Trois acteurs tests ont été créés les commandes suivantes permettent de voir leurs interractions.

```
Write-Host "`n=== TEST 1: Initialisation ===" -ForegroundColor Cyan
Invoke-WebRequest -Uri http://localhost:8080/api/actors/init -Method POST

Start-Sleep -Seconds 1

Write-Host "`n=== TEST 2: Liste des acteurs ===" -ForegroundColor Cyan
Invoke-WebRequest -Uri http://localhost:8080/api/actors/list -Method GET

Start-Sleep -Seconds 1

Write-Host "`n=== TEST 3: Commande simple ===" -ForegroundColor Cyan
$body1 = '{"items": ["Laptop"]}'
$result1 = Invoke-WebRequest -Uri http://localhost:8080/api/actors/order -Method POST -ContentType "application/json" -Body $body1
$result1.Content

Start-Sleep -Seconds 1

Write-Host "`n=== TEST 4: Commande multiple ===" -ForegroundColor Cyan
$body2 = '{"items": ["Phone", "Tablet", "Watch"]}'
$result2 = Invoke-WebRequest -Uri http://localhost:8080/api/actors/order -Method POST -ContentType "application/json" -Body $body2
$result2.Content

Start-Sleep -Seconds 1

Write-Host "`n=== TEST 5: Commande vide (échec attendu) ===" -ForegroundColor Cyan
$body3 = '{"items": []}'
$result3 = Invoke-WebRequest -Uri http://localhost:8080/api/actors/order -Method POST -ContentType "application/json" -Body $body3
$result3.Content

Start-Sleep -Seconds 1

Write-Host "`n=== LOGS (30 dernières lignes) ===" -ForegroundColor Yellow
docker logs --tail 30 $(docker ps -q)
```


# État actuel du projet
- Framework d'acteurs maison: création/destruction, blocage ASK, mailbox, dead letters, history, supervision basique (Supervisor, Hypervisor).
- Messaging intra/inter services: TELL/ASK HTTP, blocage/déblocage ASK, dead letters, historique; liaison CompletableFuture pour ASK remote.
- Observabilité: endpoints /api/metrics/*, /actuator/health, /actuator/prometheus, flux SSE, pages /observability et /dashboard (démo).
- Routers: round-robin et broadcast, démo via RouterTest (console).
- Tests: ActorSystemTests OK; MessageServiceTest OK (profil test/local); test de contexte Spring désactivé (inutile ici).
- Cluster Akka: optionnel(selon le fichier de la prof), nécessite deux nœuds cohérents (2551/2552) si activé; sinon, privilégier provider local/test.

# À faire / manquants
- Découverte de services (Eureka) au lieu des URLs statiques.
- Appli de démo distribuée demandée (flight radar) avec 2 microservices distincts + scripts/tests d’intégration.
- Logs fichiers (Logback + rotation) avec traceId/msgId.
- Supervision avancée: stratégies restart/stop/escalate + tests; tolérance pannes distribuée.
- Scalabilité/élasticité: auto-spawn ou router adaptatif selon backlog/charge.
- Tests d’intégration (comm intra/inter, supervision, health/metrics, démo).
- Documentation: README enrichi, slides PDF/LaTeX, collection Postman, scripts de test reproductibles.
- Si cluster Akka conservé: aligner seeds et lancement 2 nœuds; sinon le désactiver en prod/test.

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
