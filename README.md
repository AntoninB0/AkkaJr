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
