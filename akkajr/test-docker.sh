#!/bin/bash

# Script pour exécuter les tests dans Docker

set -e

echo "=========================================="
echo "  Tests AkkaJr dans Docker"
echo "=========================================="
echo ""

# Couleurs pour la sortie
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Fonction pour afficher les options
show_menu() {
    echo "Choisissez le type de tests à exécuter :"
    echo ""
    echo "1) Tous les tests (unitaires + intégration)"
    echo "2) Tests d'intégration uniquement"
    echo "3) Tests unitaires uniquement"
    echo "4) Test spécifique (entrez le nom de la classe)"
    echo "5) Tests avec rapport détaillé"
    echo ""
    read -p "Votre choix (1-5): " choice
}

# Exécuter selon le choix
case $1 in
    all|"")
        echo -e "${BLUE}Exécution de tous les tests...${NC}"
        docker-compose -f docker-compose.test.yml run --rm test-runner
        ;;
    integration)
        echo -e "${BLUE}Exécution des tests d'intégration...${NC}"
        docker-compose -f docker-compose.test.yml run --rm integration-tests
        ;;
    unit)
        echo -e "${BLUE}Exécution des tests unitaires...${NC}"
        docker-compose -f docker-compose.test.yml run --rm unit-tests
        ;;
    specific)
        if [ -z "$2" ]; then
            echo "Usage: ./test-docker.sh specific <TestClassName>"
            exit 1
        fi
        echo -e "${BLUE}Exécution du test: $2${NC}"
        docker-compose -f docker-compose.test.yml run --rm test-runner mvn test -Dtest="$2"
        ;;
    report)
        echo -e "${BLUE}Exécution des tests avec rapport détaillé...${NC}"
        docker-compose -f docker-compose.test.yml run --rm test-runner mvn clean test surefire-report:report
        echo -e "${GREEN}Rapport disponible dans: target/site/surefire-report.html${NC}"
        ;;
    *)
        show_menu
        case $choice in
            1)
                docker-compose -f docker-compose.test.yml run --rm test-runner
                ;;
            2)
                docker-compose -f docker-compose.test.yml run --rm integration-tests
                ;;
            3)
                docker-compose -f docker-compose.test.yml run --rm unit-tests
                ;;
            4)
                read -p "Nom de la classe de test: " test_class
                docker-compose -f docker-compose.test.yml run --rm test-runner mvn test -Dtest="$test_class"
                ;;
            5)
                docker-compose -f docker-compose.test.yml run --rm test-runner mvn clean test surefire-report:report
                echo -e "${GREEN}Rapport disponible dans: target/site/surefire-report.html${NC}"
                ;;
            *)
                echo "Choix invalide"
                exit 1
                ;;
        esac
        ;;
esac

echo ""
echo -e "${GREEN}Tests terminés!${NC}"

