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

# Tests du système d'acteurs

Exécuter uniquement les tests du système d'acteurs (Windows PowerShell) :

```sh
cd ./akkajr/
./mvnw.cmd test -Dtest=ActorSystemTests
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
