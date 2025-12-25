# AkkaJr - Framework d'Acteurs Distribués

> Framework Java inspiré d'Akka pour la création de systèmes distribués basés sur le modèle d'acteurs, avec support de la messagerie inter-services, routage dynamique, et observabilité complète.

---

## 📋 Table des Matières

1. [Vue d'ensemble](#vue-densemble)
2. [Fonctionnalités Implémentées](#fonctionnalités-implémentées)
3. [Installation](#installation)
4. [Guide d'Utilisation par Partie](#guide-dutilisation-par-partie)
5. [API Documentation](#api-documentation)
6. [Exemples d'Utilisation](#exemples-dutilisation)
7. [Monitoring & Observabilité](#monitoring--observabilité)
8. [Tests](#tests)

---

## 🎯 Vue d'ensemble

AkkaJr est un framework complet pour la création de systèmes distribués basés sur le modèle d'acteurs. Il fournit :

- ✅ **Système d'acteurs complet** avec lifecycle management
- ✅ **Messagerie synchrone (ASK) et asynchrone (TELL)**
- ✅ **Communication inter-services** via HTTP
- ✅ **Gestion dynamique des workers** avec CRUD complet
- ✅ **Routage intelligent** (Round-Robin, Load Balancing)
- ✅ **Filtrage et recherche avancée** par tags
- ✅ **Health checks et supervision** automatique
- ✅ **Résilience** (Circuit Breaker, Retry Policy)
- ✅ **Observabilité complète** (métriques, alertes, dashboards)

---

## ✅ Fonctionnalités Implémentées

### **PARTIE 1 : Système d'Acteurs (Messages Sync/Async)** ✅
- Création et gestion d'acteurs
- Communication TELL (asynchrone)
- Communication ASK (synchrone)
- Gestion du lifecycle (preStart, receive, postStop)
- Supervisor actors avec restart automatique

### **PARTIE 2 : Gestion des Workers (Enregistrement, CRUD)** ✅
- Enregistrement de workers
- CRUD complet (Create, Read, Update, Delete)
- Gestion des métadonnées et tags
- Recherche par tags
- Statuts (AVAILABLE, BUSY, UNAVAILABLE, OFFLINE)

### **PARTIE 3 : Health Checks & Supervision** ✅
- Système de heartbeat
- Détection de pannes
- Supervision automatique
- Health checks périodiques
- Auto-restart des services

### **PARTIE 4 : Routage Dynamique (Round-Robin, Load Balancing)** ✅
- Round-Robin dynamique
- Load Balancing basé sur la charge
- Sélection intelligente de workers
- Exclusion automatique des workers indisponibles

### **PARTIE 5 : Filtrage & Recherche Avancée** ✅
- Recherche par tags
- Filtrage dans le routage
- Multi-critères de recherche
- Filtrage combiné avec routage

### **PARTIE 6 : Gestion des États & Résilience** ✅
- Transitions d'états
- Circuit Breaker
- Retry Policy avec backoff exponentiel
- Gestion des erreurs
- Dead Letter Mailbox

### **PARTIE 7 : Performance & Scalabilité** ✅
- Support de multiples workers
- Distribution équitable de charge
- Métriques de performance
- Scalabilité horizontale

---

## 🚀 Installation

### Prérequis
- Java 21+
- Maven 3.6+
- Docker (optionnel)

### Installation Locale

```bash
# Cloner le projet
git clone <repository-url>
cd AkkaJr/akkajr

# Compiler
./mvnw clean install

# Lancer l'application
./mvnw spring-boot:run
```

### Installation avec Docker

```bash
# Build l'image
cd akkajr
docker build -t akkajr .

# Lancer le conteneur
docker run -p 8080:8080 akkajr

# Accéder à l'application
open http://localhost:8080
```

### Arrêter les conteneurs Docker

```bash
# Arrêter tous les conteneurs
docker stop $(docker ps -q)

# Supprimer les conteneurs arrêtés
docker container prune -f

# Puis relancer
docker run -p 8080:8080 akkajr
```

---

## 📚 Guide d'Utilisation par Partie

### **PARTIE 1 : Système d'Acteurs**

#### 1.1 Créer un Acteur

**Via SupervisorActor (recommandé) :**

```bash
# Initialiser les acteurs
curl -X POST http://localhost:8080/api/actors/init

# Créer une commande (utilise OrderActor)
curl -X POST http://localhost:8080/api/actors/order \
  -H "Content-Type: application/json" \
  -d '{"items":["Phone","Tablet"]}'
```

**Via API directe :**

```bash
# Lister tous les acteurs
curl http://localhost:8080/api/actors/list
```

#### 1.2 Communication Asynchrone (TELL)

```bash
# Envoyer un message TELL
curl -X POST http://localhost:8080/api/messages/tell \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "actor1",
    "receiverId": "actor2",
    "content": "Hello from actor1"
  }'
```

#### 1.3 Communication Synchrone (ASK)

```bash
# Envoyer un message ASK (attend une réponse)
curl -X POST http://localhost:8080/api/messages/ask \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "actor1",
    "receiverId": "actor2",
    "content": "What is your status?"
  }'

# Répondre à un ASK
curl -X POST "http://localhost:8080/api/messages/reply?agentId=actor2" \
  -H "Content-Type: text/plain" \
  -d "Status: OK"
```

#### 1.4 Vérifier les Messages

```bash
# Voir la boîte de réception d'un acteur
curl http://localhost:8080/api/messages/inbox/actor2

# Voir l'historique des messages
curl http://localhost:8080/api/messages/history

# Voir les statistiques
curl http://localhost:8080/api/messages/stats
```

---

### **PARTIE 2 : Gestion des Workers**

#### 2.1 Enregistrer un Worker

```bash
# Enregistrement simple
curl -X POST http://localhost:8080/api/workers/register \
  -H "Content-Type: application/json" \
  -d '{
    "workerId": "worker-1",
    "address": "http://localhost:9001",
    "metadata": {}
  }'

# Enregistrement avec métadonnées
curl -X POST http://localhost:8080/api/workers/register \
  -H "Content-Type: application/json" \
  -d '{
    "workerId": "worker-payment",
    "address": "http://localhost:9002",
    "metadata": {
      "region": "eu-west-1",
      "capability": "payment",
      "tier": "premium",
      "version": "2.0.1"
    }
  }'
```

#### 2.2 Lire les Workers

```bash
# Lister tous les workers
curl http://localhost:8080/api/workers

# Lister uniquement les workers disponibles
curl http://localhost:8080/api/workers/available

# Obtenir un worker spécifique
curl http://localhost:8080/api/workers/worker-1
```

#### 2.3 Mettre à Jour un Worker

```bash
# Changer le statut
curl -X PUT http://localhost:8080/api/workers/worker-1/status \
  -H "Content-Type: application/json" \
  -d '{
    "status": "BUSY"
  }'

# Statuts disponibles: AVAILABLE, BUSY, UNAVAILABLE, OFFLINE
```

#### 2.4 Supprimer un Worker

```bash
curl -X DELETE http://localhost:8080/api/workers/worker-1
```

#### 2.5 Rechercher des Workers

```bash
# Recherche par tag
curl "http://localhost:8080/api/workers/search?tag=region&value=eu-west-1"
```

---

### **PARTIE 3 : Health Checks & Supervision**

#### 3.1 Envoyer un Heartbeat

```bash
# Envoyer un heartbeat pour un worker
curl -X POST http://localhost:8080/api/workers/worker-1/heartbeat
```

#### 3.2 Vérifier la Santé

```bash
# Health check global
curl http://localhost:8080/actuator/health

# Métriques des acteurs
curl http://localhost:8080/api/metrics/actors
```

#### 3.3 Supervision Automatique

La supervision est automatique. Le système :
- Vérifie périodiquement les heartbeats
- Détecte les workers morts
- Met à jour automatiquement les statuts
- Exclut les workers OFFLINE du routage

---

### **PARTIE 4 : Routage Dynamique**

#### 4.1 Round-Robin

```bash
# Sélection Round-Robin basique
curl -X POST http://localhost:8080/api/router/dynamic/roundrobin

# Round-Robin avec filtre
curl -X POST http://localhost:8080/api/router/dynamic/roundrobin \
  -H "Content-Type: application/json" \
  -d '{
    "filter": {
      "tag": "region",
      "value": "eu-west-1"
    }
  }'
```

#### 4.2 Load Balancing

```bash
# Sélection Load Balanced (choisit le worker avec la charge la plus faible)
curl -X POST http://localhost:8080/api/router/dynamic/loadbalanced

# Load Balancing avec filtre
curl -X POST http://localhost:8080/api/router/dynamic/loadbalanced \
  -H "Content-Type: application/json" \
  -d '{
    "filter": {
      "tag": "capability",
      "value": "payment"
    }
  }'
```

#### 4.3 Démonstration des Stratégies

```bash
# Tester les deux stratégies
curl -X POST http://localhost:8080/api/router/dynamic/demo
```

**Réponse :**
```json
{
  "roundRobin": {
    "success": true,
    "workerId": "worker-1",
    "workerAddress": "http://localhost:9001"
  },
  "loadBalanced": {
    "success": true,
    "workerId": "worker-2",
    "workerAddress": "http://localhost:9002",
    "load": 0
  }
}
```

---

### **PARTIE 5 : Filtrage & Recherche Avancée**

#### 5.1 Recherche par Tag

```bash
# Recherche simple
curl "http://localhost:8080/api/workers/search?tag=region&value=us-east"

# Recherche par capability
curl "http://localhost:8080/api/workers/search?tag=capability&value=payment"
```

#### 5.2 Routage avec Filtre

```bash
# Round-Robin avec filtre de région
curl -X POST http://localhost:8080/api/router/dynamic/roundrobin \
  -H "Content-Type: application/json" \
  -d '{
    "filter": {
      "tag": "region",
      "value": "us-east"
    }
  }'

# Load Balancing avec filtre de capability
curl -X POST http://localhost:8080/api/router/dynamic/loadbalanced \
  -H "Content-Type: application/json" \
  -d '{
    "filter": {
      "tag": "capability",
      "value": "payment"
    }
  }'
```

---

### **PARTIE 6 : Gestion des États & Résilience**

#### 6.1 Transitions d'États

```bash
# AVAILABLE → BUSY
curl -X PUT http://localhost:8080/api/workers/worker-1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "BUSY"}'

# BUSY → AVAILABLE
curl -X PUT http://localhost:8080/api/workers/worker-1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "AVAILABLE"}'

# AVAILABLE → OFFLINE
curl -X PUT http://localhost:8080/api/workers/worker-1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "OFFLINE"}'
```

#### 6.2 Gestion des Erreurs

```bash
# Voir les dead letters (messages non livrés)
curl http://localhost:8080/api/messages/deadletters

# Voir les logs des messages
curl http://localhost:8080/api/messages/logs
```

#### 6.3 Résilience

Le système gère automatiquement :
- **Circuit Breaker** : Détection de pannes répétées
- **Retry Policy** : Tentatives avec backoff exponentiel
- **Dead Letter Mailbox** : Messages non livrés sauvegardés

---

### **PARTIE 7 : Performance & Scalabilité**

#### 7.1 Enregistrer Plusieurs Workers

```bash
# Enregistrer 10 workers
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/workers/register \
    -H "Content-Type: application/json" \
    -d "{
      \"workerId\": \"worker-$i\",
      \"address\": \"http://localhost:900$i\",
      \"metadata\": {
        \"region\": \"us-east\",
        \"tier\": \"standard\"
      }
    }"
done
```

#### 7.2 Tests de Performance

```bash
# 20 sélections Round-Robin consécutives
for i in {1..20}; do
  curl -X POST http://localhost:8080/api/router/dynamic/roundrobin
done

# 20 sélections Load Balanced consécutives
for i in {1..20}; do
  curl -X POST http://localhost:8080/api/router/dynamic/loadbalanced
done
```

---

## 📖 API Documentation

### **Acteurs (`/api/actors`)**

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/actors/init` | Initialiser les acteurs de démo |
| POST | `/api/actors/order` | Créer une commande |
| POST | `/api/actors/payment` | Traiter un paiement |
| POST | `/api/actors/notify` | Envoyer une notification |
| GET | `/api/actors/list` | Lister tous les acteurs |

### **Messages (`/api/messages`)**

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/messages/tell` | Envoyer un message TELL (async) |
| POST | `/api/messages/ask` | Envoyer un message ASK (sync) |
| POST | `/api/messages/reply` | Répondre à un ASK |
| GET | `/api/messages/inbox/{agentId}` | Boîte de réception d'un agent |
| GET | `/api/messages/history` | Historique des messages |
| GET | `/api/messages/deadletters` | Messages non livrés |
| GET | `/api/messages/stats` | Statistiques des messages |
| GET | `/api/messages/logs` | Logs des messages |

### **Workers (`/api/workers`)**

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/workers/register` | Enregistrer un worker |
| GET | `/api/workers` | Lister tous les workers |
| GET | `/api/workers/available` | Lister les workers disponibles |
| GET | `/api/workers/{workerId}` | Obtenir un worker spécifique |
| PUT | `/api/workers/{workerId}/status` | Mettre à jour le statut |
| POST | `/api/workers/{workerId}/heartbeat` | Envoyer un heartbeat |
| DELETE | `/api/workers/{workerId}` | Supprimer un worker |
| GET | `/api/workers/search` | Rechercher par tag |

### **Routage (`/api/router/dynamic`)**

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/router/dynamic/roundrobin` | Sélection Round-Robin |
| POST | `/api/router/dynamic/loadbalanced` | Sélection Load Balanced |
| POST | `/api/router/dynamic/demo` | Démonstration des stratégies |

### **Métriques (`/api/metrics`)**

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/metrics/actors` | Métriques agrégées des acteurs |
| GET | `/api/metrics/actors/detail` | Détails par acteur |
| GET | `/api/metrics/events` | Événements récents |
| GET | `/api/metrics/alerts` | Alertes actuelles |
| GET | `/api/metrics/stream` | Stream SSE des métriques |

### **Actuator**

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/actuator/health` | Health check |
| GET | `/actuator/prometheus` | Métriques Prometheus |

---

## 💡 Exemples d'Utilisation

### **Exemple Complet : E-commerce**

```bash
# 1. Initialiser les acteurs
curl -X POST http://localhost:8080/api/actors/init

# 2. Créer une commande
curl -X POST http://localhost:8080/api/actors/order \
  -H "Content-Type: application/json" \
  -d '{"items":["Laptop","Mouse"]}'

# 3. Enregistrer des workers de paiement
curl -X POST http://localhost:8080/api/workers/register \
  -H "Content-Type: application/json" \
  -d '{
    "workerId": "payment-worker-1",
    "address": "http://localhost:9001",
    "metadata": {
      "capability": "payment",
      "region": "eu-west"
    }
  }'

# 4. Sélectionner un worker pour le paiement
curl -X POST http://localhost:8080/api/router/dynamic/loadbalanced \
  -H "Content-Type: application/json" \
  -d '{
    "filter": {
      "tag": "capability",
      "value": "payment"
    }
  }'
```

### **Exemple : Communication Inter-Services**

```bash
# Service 1 (port 8080)
# Configuration: app.service.name=service1
# app.remote.services=service2=http://localhost:8081

# Service 2 (port 8081)
# Configuration: app.service.name=service2
# app.remote.services=service1=http://localhost:8080

# Envoyer un message TELL vers service2
curl -X POST http://localhost:8080/api/messages/tell \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "actor1",
    "receiverId": "service2:actor2",
    "content": "Hello from service1"
  }'

# Envoyer un message ASK vers service2
curl -X POST http://localhost:8080/api/messages/ask \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "actor1",
    "receiverId": "service2:actor2",
    "content": "What is your status?"
  }'
```

---

## 📊 Monitoring & Observabilité

### **Dashboard Web**

Accéder au dashboard d'observabilité :
```
http://localhost:8080/observability
```

Le dashboard affiche :
- Métriques en temps réel (SSE)
- État de chaque acteur
- Alertes système
- Feed d'événements
- Health status

### **Endpoints Principaux**

- **UI live** : `http://localhost:8080/observability` (SSE metrics/alerts, tableau par acteur, health, feed d'événements)
- **Snapshot agrégé** : `GET /api/metrics/actors`
- **Détail acteurs** : `GET /api/metrics/actors/detail` (backlog, processed, failed, paused, guardian)
- **Alertes** : `GET /api/metrics/alerts`
- **Stream SSE** : `GET /api/metrics/stream`
- **Événements récents** : `GET /api/metrics/events` (msgId/traceId, processed/failed)
- **Health** : `GET /actuator/health` (porte OUT_OF_SERVICE si backlog élevé)
- **Prometheus** : `GET /actuator/prometheus` (toutes les métriques Micrometer, y compris per-actor)

### **Métriques Prometheus**

```bash
# Exporter les métriques
curl http://localhost:8080/actuator/prometheus
```

### **Ce qui est Instrumenté**

- **Compteurs** : acteurs créés/stoppés, messages processed/failed par acteur, backlog et paused agrégés
- **Latence** : timer Micrometer par acteur (exposé dans `/actuator/prometheus`)
- **Health** : backlog élevé => OUT_OF_SERVICE via ActorSystemHealthIndicator
- **Traçabilité** : msgId/traceId dans logs et dans `/api/metrics/events` (feed UI)
- **Alertes** : backlog/paused/messages_failed surfacent dans SSE et UI

---

## 🧪 Tests

### **Tests Unitaires**

```bash
cd akkajr
./mvnw test
```

### **Tests d'Intégration**

```bash
# Tests complets par parties
cd akkajr
PowerShell -ExecutionPolicy Bypass -File .\test-complet-par-parties.ps1

# Tests simples
PowerShell -ExecutionPolicy Bypass -File .\test-complet-simple.ps1
```

### **Tests dans Docker**

#### **Option 1 : Script Automatique (Recommandé)**

```bash
cd akkajr

# Tous les tests
./test-docker.sh all

# Tests d'intégration uniquement
./test-docker.sh integration

# Tests unitaires uniquement
./test-docker.sh unit

# Test spécifique
./test-docker.sh specific WorkerManagementIntegrationTest

# Tests avec rapport détaillé
./test-docker.sh report
```

#### **Option 2 : Docker Compose**

```bash
cd akkajr

# Tous les tests
docker-compose -f docker-compose.test.yml run --rm test-runner

# Tests d'intégration uniquement
docker-compose -f docker-compose.test.yml run --rm integration-tests

# Tests unitaires uniquement
docker-compose -f docker-compose.test.yml run --rm unit-tests

# Test spécifique
docker-compose -f docker-compose.test.yml run --rm test-runner mvn test -Dtest="WorkerManagementIntegrationTest"
```

#### **Option 3 : Docker Direct**

```bash
cd akkajr

# Build l'image de test
docker build -f Dockerfile.test -t akkajr-test .

# Exécuter tous les tests
docker run --rm -v $(pwd):/app akkajr-test mvn clean test

# Exécuter les tests d'intégration
docker run --rm -v $(pwd):/app akkajr-test mvn clean test -Dtest="*IntegrationTest"

# Exécuter un test spécifique
docker run --rm -v $(pwd):/app akkajr-test mvn test -Dtest="WorkerManagementIntegrationTest"
```

#### **Voir les Rapports de Tests**

Après l'exécution des tests, les rapports sont disponibles dans :
- `target/surefire-reports/` - Rapports XML et TXT
- `target/site/surefire-report.html` - Rapport HTML (après `mvn surefire-report:report`)

Pour générer le rapport HTML :
```bash
docker-compose -f docker-compose.test.yml run --rm test-runner mvn surefire-report:report
```

### **Lancer l'Application Localement**

```bash
cd akkajr
./mvnw spring-boot:run
# Ouvrir /observability pour le dashboard
```

---

## 🔧 Configuration

### **application.properties**

```properties
# Service
server.port=8080
app.service.name=service1
app.remote.services=service2=http://localhost:8081

# Akka
akka.port=2551

# Hypervisor
hypervisor.healthcheck.interval=10000
hypervisor.heartbeat.timeout=30000

# Logging
logging.level.com.example.akkajr=DEBUG
```

### **Multi-Service Setup**

**Service 1 :**
```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=8080 --app.service.name=service1 --app.remote.services=service2=http://localhost:8081"
```

**Service 2 :**
```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=8081 --app.service.name=service2 --app.remote.services=service1=http://localhost:8080"
```

### **Commandes Utiles (Local)**

```bash
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
```

---

## 📝 Structure du Projet

```
akkajr/
├── src/main/java/com/example/akkajr/
│   ├── core/
│   │   ├── actors/          # Système d'acteurs
│   │   ├── metrics/         # Métriques
│   │   └── observability/   # Observabilité
│   ├── messaging/           # Système de messagerie
│   ├── router/              # Routage dynamique
│   └── controllers/         # API REST
├── src/main/resources/
│   └── application.properties
└── test-complet-par-parties.ps1
```


## 📚 Ressources

- **Documentation Spring Boot** : https://spring.io/projects/spring-boot
- **Modèle d'Acteurs** : https://en.wikipedia.org/wiki/Actor_model
- **Akka (inspiration)** : https://akka.io/

### **VS Code Extensions Recommandées**

- https://marketplace.visualstudio.com/items?itemName=Al-rimi.tomcat
- https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-test
- https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-spring-initializr
- https://marketplace.visualstudio.com/items?itemName=vmware.vscode-spring-boot
- https://marketplace.visualstudio.com/items?itemName=vmware.vscode-boot-dev-pack
- https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-spring-boot-dashboard
- https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-dependency
- https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-maven
- https://marketplace.visualstudio.com/items?itemName=redhat.java
- https://marketplace.visualstudio.com/items?itemName=Oracle.oracle-java
- https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-gradle
- https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack
- https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-debug
- https://marketplace.visualstudio.com/items?itemName=ms-azuretools.vscode-containers

---

## 👥 Auteurs

Afdali, Aguel, Ben Mansour, Bo, Bonnet, Brouziyne
---

## 📄 Licence

Ce projet est un projet académique.

---

**Version** : 1.0.0  
**Dernière mise à jour** : 2025-12-24