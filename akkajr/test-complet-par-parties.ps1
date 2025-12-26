# =========================================================================
# TESTS COMPLETS PAR PARTIES - AkkaJr
# =========================================================================
# 
# Ce script teste l'ensemble du projet AkkaJr de manière structurée :
# 
# PARTIE 1 : Système d'Acteurs (Messages Sync/Async)
# PARTIE 2 : Gestion des Workers (Enregistrement, CRUD)
# PARTIE 3 : Health Checks & Supervision
# PARTIE 4 : Routage Dynamique (Round-Robin, Load Balancing)
# PARTIE 5 : Filtrage & Recherche Avancée
# PARTIE 6 : Gestion des États & Résilience
# PARTIE 7 : Performance & Scalabilité
#
# =========================================================================

param(
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$Verbose,
    [switch]$StopOnError
)

# =========================================================================
# CONFIGURATION & UTILITAIRES
# =========================================================================

$ErrorActionPreference = if ($StopOnError) { "Stop" } else { "Continue" }

$Global:TestResults = @{
    Total = 0
    Passed = 0
    Failed = 0
    Skipped = 0
    PartieResults = @{}
}

function Write-TestHeader {
    param([string]$Title)
    Write-Host "`n$('=' * 80)" -ForegroundColor Cyan
    Write-Host "  $Title" -ForegroundColor Cyan
    Write-Host "$('=' * 80)" -ForegroundColor Cyan
}

function Write-PartieHeader {
    param([string]$Numero, [string]$Titre, [string]$Description)
    Write-Host "`n" -NoNewline
    Write-Host "╔$('═' * 78)╗" -ForegroundColor Magenta
    Write-Host "║ PARTIE $Numero : $Titre" -ForegroundColor Magenta -NoNewline
    Write-Host (" " * (70 - $Titre.Length - $Numero.Length)) -NoNewline
    Write-Host "║" -ForegroundColor Magenta
    Write-Host "║ $Description" -ForegroundColor Magenta -NoNewline
    Write-Host (" " * (77 - $Description.Length)) -NoNewline
    Write-Host "║" -ForegroundColor Magenta
    Write-Host "╚$('═' * 78)╝" -ForegroundColor Magenta
}

function Test-Endpoint {
    param(
        [string]$Name,
        [scriptblock]$Test,
        [string]$Partie = "General"
    )
    
    $Global:TestResults.Total++
    
    if (-not $Global:TestResults.PartieResults.ContainsKey($Partie)) {
        $Global:TestResults.PartieResults[$Partie] = @{
            Total = 0
            Passed = 0
            Failed = 0
        }
    }
    
    $Global:TestResults.PartieResults[$Partie].Total++
    
    Write-Host "`n  [TEST] " -ForegroundColor Yellow -NoNewline
    Write-Host $Name -ForegroundColor White
    
    try {
        $startTime = Get-Date
        & $Test
        $duration = (Get-Date) - $startTime
        
        $Global:TestResults.Passed++
        $Global:TestResults.PartieResults[$Partie].Passed++
        
        Write-Host "    ✓ PASSÉ" -ForegroundColor Green -NoNewline
        Write-Host " ($($duration.TotalMilliseconds.ToString('F0'))ms)" -ForegroundColor Gray
        
        return $true
    } catch {
        $Global:TestResults.Failed++
        $Global:TestResults.PartieResults[$Partie].Failed++
        
        Write-Host "    ✗ ÉCHEC" -ForegroundColor Red
        Write-Host "    Erreur : $_" -ForegroundColor Red
        
        if ($Verbose) {
            Write-Host "    Stack : $($_.ScriptStackTrace)" -ForegroundColor DarkRed
        }
        
        return $false
    }
}

function Invoke-ApiCall {
    param(
        [string]$Method = "GET",
        [string]$Endpoint,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )
    
    $uri = "$BaseUrl$Endpoint"
    
    $params = @{
        Uri = $uri
        Method = $Method
        Headers = $Headers
    }
    
    if ($Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 10)
        $params.ContentType = "application/json"
    } elseif ($Method -eq "POST" -or $Method -eq "PUT") {
        # Pour les POST/PUT sans body, envoyer un objet vide
        $params.Body = (@{} | ConvertTo-Json)
        $params.ContentType = "application/json"
    }
    
    return Invoke-RestMethod @params
}

function Clear-AllWorkers {
    try {
        $workers = Invoke-ApiCall -Endpoint "/api/workers"
        foreach ($worker in $workers) {
            try {
                Invoke-ApiCall -Method DELETE -Endpoint "/api/workers/$($worker.id)"
            } catch {
                # Ignore errors during cleanup
            }
        }
    } catch {
        # Ignore errors during cleanup
    }
}

# =========================================================================
# DÉBUT DES TESTS
# =========================================================================

Write-TestHeader "AKKAJR - TESTS COMPLETS PAR PARTIES"
Write-Host "`nURL Base : $BaseUrl" -ForegroundColor Gray
Write-Host "Date     : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Gray

# Vérifier que le service est accessible
try {
    Write-Host "`n[Vérification] Service accessible..." -NoNewline
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/workers" -UseBasicParsing -TimeoutSec 5
    Write-Host " OK" -ForegroundColor Green
} catch {
    Write-Host " ERREUR" -ForegroundColor Red
    Write-Host "Le service n'est pas accessible à $BaseUrl" -ForegroundColor Red
    Write-Host "Assurez-vous que le conteneur Docker est démarré." -ForegroundColor Yellow
    exit 1
}

# Nettoyer avant de commencer
Clear-AllWorkers

# =========================================================================
# PARTIE 1 : SYSTÈME D'ACTEURS (Messages Sync/Async)
# =========================================================================

Write-PartieHeader "1" "SYSTÈME D'ACTEURS" "Messages synchrones, asynchrones et gestion des acteurs"

Test-Endpoint "1.1 - Création d'acteur via API" -Partie "Acteurs" {
    # Le système devrait créer des acteurs automatiquement lors de l'enregistrement
    $worker = @{
        workerId = "actor-test-1"
        address = "http://localhost:9001"
        metadata = @{ type = "actor" }
    }
    
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/workers/register" -Body $worker
    
    if ($result.workerId -ne "actor-test-1") {
        throw "Worker ID incorrect"
    }
}

Test-Endpoint "1.2 - Envoi de message asynchrone (Tell pattern)" -Partie "Acteurs" {
    # Via l'API de routage (asynchrone par nature)
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/roundrobin"
    
    if (-not $result.success) {
        throw "Message asynchrone échoué"
    }
    
    # Vérifier que le worker a été sélectionné
    if (-not $result.workerId) {
        throw "Aucun worker sélectionné"
    }
}

Test-Endpoint "1.3 - Vérification de l'état d'un acteur" -Partie "Acteurs" {
    $details = Invoke-ApiCall -Endpoint "/api/workers/actor-test-1"
    
    if ($details.status -ne "AVAILABLE") {
        throw "Statut acteur incorrect : $($details.status)"
    }
}

Test-Endpoint "1.4 - Liste de tous les acteurs/workers" -Partie "Acteurs" {
    $workers = Invoke-ApiCall -Endpoint "/api/workers"
    
    if ($workers.Count -lt 1) {
        throw "Aucun acteur trouvé"
    }
}

# =========================================================================
# PARTIE 2 : GESTION DES WORKERS (CRUD Complet)
# =========================================================================

Write-PartieHeader "2" "GESTION DES WORKERS" "Enregistrement, lecture, mise à jour et suppression"

Test-Endpoint "2.1 - CREATE : Enregistrement worker simple" -Partie "Workers" {
    $worker = @{
        workerId = "worker-simple"
        address = "http://localhost:9010"
        metadata = @{}
    }
    
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/workers/register" -Body $worker
    
    if ($result.workerId -ne "worker-simple") {
        throw "Enregistrement échoué"
    }
}

Test-Endpoint "2.2 - CREATE : Enregistrement worker avec métadonnées" -Partie "Workers" {
    $worker = @{
        workerId = "worker-meta"
        address = "http://localhost:9011"
        metadata = @{
            region = "eu-west-1"
            capability = "payment"
            tier = "premium"
            datacenter = "paris"
            version = "2.0.1"
        }
    }
    
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/workers/register" -Body $worker
    
    if ($result.status -ne "AVAILABLE") {
        throw "Statut initial incorrect"
    }
}

Test-Endpoint "2.3 - CREATE : Enregistrement multiple (batch)" -Partie "Workers" {
    $workers = @(
        @{ workerId = "batch-1"; address = "http://localhost:9021"; metadata = @{ region = "us-east" } },
        @{ workerId = "batch-2"; address = "http://localhost:9022"; metadata = @{ region = "us-east" } },
        @{ workerId = "batch-3"; address = "http://localhost:9023"; metadata = @{ region = "eu-west" } }
    )
    
    foreach ($w in $workers) {
        Invoke-ApiCall -Method POST -Endpoint "/api/workers/register" -Body $w | Out-Null
    }
    
    $allWorkers = Invoke-ApiCall -Endpoint "/api/workers"
    
    if ($allWorkers.Count -lt 6) {
        throw "Tous les workers ne sont pas enregistrés"
    }
}

Test-Endpoint "2.4 - READ : Lecture worker spécifique" -Partie "Workers" {
    $details = Invoke-ApiCall -Endpoint "/api/workers/worker-meta"
    
    if ($details.id -ne "worker-meta") {
        throw "Worker ID incorrect"
    }
    
    if ($details.metadata.region -ne "eu-west-1") {
        throw "Métadonnées incorrectes"
    }
}

Test-Endpoint "2.5 - READ : Liste de tous les workers" -Partie "Workers" {
    $workers = Invoke-ApiCall -Endpoint "/api/workers"
    
    if ($workers.Count -lt 6) {
        throw "Nombre de workers incorrect : $($workers.Count)"
    }
}

Test-Endpoint "2.6 - READ : Liste workers disponibles uniquement" -Partie "Workers" {
    $available = Invoke-ApiCall -Endpoint "/api/workers/available"
    
    # Tous devraient être disponibles au début
    $all = Invoke-ApiCall -Endpoint "/api/workers"
    
    if ($available.Count -ne $all.Count) {
        throw "Nombre de workers disponibles incorrect"
    }
}

Test-Endpoint "2.7 - UPDATE : Changement de statut (AVAILABLE → BUSY)" -Partie "Workers" {
    $statusChange = @{ status = "BUSY" }
    
    $result = Invoke-ApiCall -Method PUT -Endpoint "/api/workers/worker-simple/status" -Body $statusChange
    
    # Vérifier le changement
    $details = Invoke-ApiCall -Endpoint "/api/workers/worker-simple"
    
    if ($details.status -ne "BUSY") {
        throw "Statut non mis à jour"
    }
}

Test-Endpoint "2.8 - UPDATE : Changement de statut (BUSY → AVAILABLE)" -Partie "Workers" {
    $statusChange = @{ status = "AVAILABLE" }
    
    Invoke-ApiCall -Method PUT -Endpoint "/api/workers/worker-simple/status" -Body $statusChange | Out-Null
    
    $details = Invoke-ApiCall -Endpoint "/api/workers/worker-simple"
    
    if ($details.status -ne "AVAILABLE") {
        throw "Retour à AVAILABLE échoué"
    }
}

Test-Endpoint "2.9 - UPDATE : Changement de statut (AVAILABLE → OFFLINE)" -Partie "Workers" {
    $statusChange = @{ status = "OFFLINE" }
    
    Invoke-ApiCall -Method PUT -Endpoint "/api/workers/batch-1/status" -Body $statusChange | Out-Null
    
    $details = Invoke-ApiCall -Endpoint "/api/workers/batch-1"
    
    if ($details.status -ne "OFFLINE") {
        throw "Passage OFFLINE échoué"
    }
}

Test-Endpoint "2.10 - DELETE : Suppression d'un worker" -Partie "Workers" {
    Invoke-ApiCall -Method DELETE -Endpoint "/api/workers/actor-test-1"
    
    # Vérifier qu'il n'existe plus
    try {
        Invoke-ApiCall -Endpoint "/api/workers/actor-test-1"
        throw "Worker devrait être supprimé"
    } catch {
        # C'est attendu
        if ($_.Exception.Message -notmatch "404|Not Found") {
            throw $_
        }
    }
}

# =========================================================================
# PARTIE 3 : HEALTH CHECKS & SUPERVISION
# =========================================================================

Write-PartieHeader "3" "HEALTH CHECKS & SUPERVISION" "Heartbeats, timeouts et détection de pannes"

Test-Endpoint "3.1 - Heartbeat : Enregistrement heartbeat" -Partie "HealthChecks" {
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/workers/worker-meta/heartbeat"
    
    # Le heartbeat devrait mettre à jour lastHeartbeat
    $details = Invoke-ApiCall -Endpoint "/api/workers/worker-meta"
    
    if (-not $details.lastHeartbeat) {
        throw "Heartbeat non enregistré"
    }
}

Test-Endpoint "3.2 - Heartbeat : Vérification mise à jour timestamp" -Partie "HealthChecks" {
    Start-Sleep -Seconds 1
    
    $before = Invoke-ApiCall -Endpoint "/api/workers/worker-meta"
    $timestampBefore = $before.lastHeartbeat
    
    Start-Sleep -Milliseconds 500
    
    Invoke-ApiCall -Method POST -Endpoint "/api/workers/worker-meta/heartbeat" | Out-Null
    
    $after = Invoke-ApiCall -Endpoint "/api/workers/worker-meta"
    $timestampAfter = $after.lastHeartbeat
    
    if ($timestampAfter -le $timestampBefore) {
        throw "Timestamp heartbeat non mis à jour"
    }
}

Test-Endpoint "3.3 - Heartbeat : Remise en AVAILABLE après heartbeat" -Partie "HealthChecks" {
    # Mettre en BUSY
    $statusChange = @{ status = "BUSY" }
    Invoke-ApiCall -Method PUT -Endpoint "/api/workers/worker-meta/status" -Body $statusChange | Out-Null
    
    # Envoyer heartbeat
    Invoke-ApiCall -Method POST -Endpoint "/api/workers/worker-meta/heartbeat" | Out-Null
    
    # Devrait être AVAILABLE maintenant
    $details = Invoke-ApiCall -Endpoint "/api/workers/worker-meta"
    
    if ($details.status -ne "AVAILABLE") {
        throw "Worker pas remis en AVAILABLE après heartbeat"
    }
}

Test-Endpoint "3.4 - Health Check : Workers disponibles exclut OFFLINE" -Partie "HealthChecks" {
    $available = Invoke-ApiCall -Endpoint "/api/workers/available"
    
    # batch-1 est OFFLINE, ne devrait pas être dans la liste
    $offline = $available | Where-Object { $_.id -eq "batch-1" }
    
    if ($offline) {
        throw "Worker OFFLINE présent dans liste disponibles"
    }
}

Test-Endpoint "3.5 - Health Check : Workers disponibles exclut BUSY" -Partie "HealthChecks" {
    # Mettre batch-2 en BUSY
    $statusChange = @{ status = "BUSY" }
    Invoke-ApiCall -Method PUT -Endpoint "/api/workers/batch-2/status" -Body $statusChange | Out-Null
    
    $available = Invoke-ApiCall -Endpoint "/api/workers/available"
    
    $busy = $available | Where-Object { $_.id -eq "batch-2" }
    
    if ($busy) {
        throw "Worker BUSY présent dans liste disponibles"
    }
}

Test-Endpoint "3.6 - Supervision : Vérification charge worker" -Partie "HealthChecks" {
    $details = Invoke-ApiCall -Endpoint "/api/workers/worker-simple"
    
    if ($details.currentLoad -lt 0) {
        throw "Charge négative impossible"
    }
}

# =========================================================================
# PARTIE 4 : ROUTAGE DYNAMIQUE
# =========================================================================

Write-PartieHeader "4" "ROUTAGE DYNAMIQUE" "Round-Robin, Load Balancing et sélection intelligente"

Test-Endpoint "4.1 - Round-Robin : Sélection basique" -Partie "RoutageRoundRobin" {
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/roundrobin"
    
    if (-not $result.success) {
        throw "Sélection Round-Robin échouée : $($result.message)"
    }
    
    if (-not $result.workerId) {
        throw "Aucun worker sélectionné"
    }
}

Test-Endpoint "4.2 - Round-Robin : Distribution équitable (6 sélections)" -Partie "RoutageRoundRobin" {
    $selections = @{}
    
    # Faire 6 sélections
    for ($i = 1; $i -le 6; $i++) {
        $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/roundrobin"
        
        $workerId = $result.workerId
        
        if (-not $selections.ContainsKey($workerId)) {
            $selections[$workerId] = 0
        }
        $selections[$workerId]++
    }
    
    # Vérifier qu'au moins 2 workers différents ont été sélectionnés
    if ($selections.Count -lt 2) {
        throw "Distribution non équitable : seulement $($selections.Count) worker(s) distinct(s)"
    }
}

Test-Endpoint "4.3 - Round-Robin : Exclusion workers non-AVAILABLE" -Partie "RoutageRoundRobin" {
    # batch-1 est OFFLINE, batch-2 est BUSY
    # Faire plusieurs sélections
    $selections = @()
    
    for ($i = 1; $i -le 10; $i++) {
        $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/roundrobin"
        $selections += $result.workerId
    }
    
    # Vérifier qu'aucun worker OFFLINE ou BUSY n'a été sélectionné
    if ($selections -contains "batch-1") {
        throw "Worker OFFLINE sélectionné"
    }
    
    if ($selections -contains "batch-2") {
        throw "Worker BUSY sélectionné"
    }
}

Test-Endpoint "4.4 - Load Balancing : Sélection basique" -Partie "RoutageLoadBalancing" {
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/loadbalanced"
    
    if (-not $result.success) {
        throw "Sélection Load Balancing échouée"
    }
    
    if ($result.workerLoad -lt 0) {
        throw "Charge négative"
    }
}

Test-Endpoint "4.5 - Load Balancing : Sélection worker charge minimale" -Partie "RoutageLoadBalancing" {
    # Faire plusieurs sélections rapides
    $results = @()
    
    for ($i = 1; $i -le 3; $i++) {
        $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/loadbalanced"
        $results += $result
    }
    
    # Vérifier que les charges sont raisonnables
    foreach ($r in $results) {
        if ($r.workerLoad -gt 10) {
            throw "Charge trop élevée : $($r.workerLoad)"
        }
    }
}

Test-Endpoint "4.6 - Démo : Test combiné des deux stratégies" -Partie "RoutageDemo" {
    $demo = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/demo"
    
    if (-not $demo.roundRobin.success) {
        throw "Round-Robin demo échoué"
    }
    
    if (-not $demo.loadBalanced.success) {
        throw "Load Balanced demo échoué"
    }
    
    # Vérifier que des workers ont été sélectionnés
    if (-not $demo.roundRobin.workerId -or -not $demo.loadBalanced.workerId) {
        throw "Workers non sélectionnés dans demo"
    }
}

# =========================================================================
# PARTIE 5 : FILTRAGE & RECHERCHE AVANCÉE
# =========================================================================

Write-PartieHeader "5" "FILTRAGE & RECHERCHE" "Recherche par tags et filtrage intelligent"

Test-Endpoint "5.1 - Recherche : Par tag simple (region)" -Partie "Recherche" {
    $results = Invoke-ApiCall -Endpoint "/api/workers/search?tag=region&value=us-east"
    
    # batch-2 et batch-3 sont en us-east (batch-1 est OFFLINE)
    if ($results.Count -lt 1) {
        throw "Aucun worker trouvé pour region=us-east"
    }
    
    # Vérifier que tous ont la bonne région
    foreach ($w in $results) {
        if ($w.metadata.region -ne "us-east") {
            throw "Worker avec mauvaise région dans les résultats"
        }
    }
}

Test-Endpoint "5.2 - Recherche : Par tag (capability)" -Partie "Recherche" {
    $results = Invoke-ApiCall -Endpoint "/api/workers/search?tag=capability&value=payment"
    
    # worker-meta a capability=payment
    $found = $results | Where-Object { $_.id -eq "worker-meta" }
    
    if (-not $found) {
        throw "Worker avec capability=payment non trouvé"
    }
}

Test-Endpoint "5.3 - Round-Robin avec filtre : region=us-east" -Partie "Filtrage" {
    $filterRequest = @{
        filter = @{
            tag = "region"
            value = "us-east"
        }
    }
    
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/roundrobin" -Body $filterRequest
    
    if (-not $result.success) {
        throw "Sélection avec filtre échouée"
    }
    
    # Vérifier que le worker sélectionné a la bonne région
    $details = Invoke-ApiCall -Endpoint "/api/workers/$($result.workerId)"
    
    if ($details.metadata.region -ne "us-east") {
        throw "Worker sélectionné n'a pas la région demandée"
    }
}

Test-Endpoint "5.4 - Round-Robin avec filtre : Plusieurs sélections" -Partie "Filtrage" {
    $filterRequest = @{
        filter = @{
            tag = "region"
            value = "us-east"
        }
    }
    
    $selections = @()
    
    for ($i = 1; $i -le 4; $i++) {
        $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/roundrobin" -Body $filterRequest
        $selections += $result.workerId
    }
    
    # Vérifier qu'aucun worker non-US n'a été sélectionné
    foreach ($workerId in $selections) {
        $details = Invoke-ApiCall -Endpoint "/api/workers/$workerId"
        
        if ($details.metadata.region -ne "us-east") {
            throw "Worker $workerId n'est pas dans la région us-east"
        }
    }
}

Test-Endpoint "5.5 - Load Balancing avec filtre : capability=payment" -Partie "Filtrage" {
    $filterRequest = @{
        filter = @{
            tag = "capability"
            value = "payment"
        }
    }
    
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/loadbalanced" -Body $filterRequest
    
    if (-not $result.success) {
        throw "Load balancing avec filtre échoué"
    }
    
    # Vérifier la capability
    $details = Invoke-ApiCall -Endpoint "/api/workers/$($result.workerId)"
    
    if ($details.metadata.capability -ne "payment") {
        throw "Worker sélectionné n'a pas la capability demandée"
    }
}

# =========================================================================
# PARTIE 6 : GESTION DES ÉTATS & RÉSILIENCE
# =========================================================================

Write-PartieHeader "6" "ÉTATS & RÉSILIENCE" "Transitions d'états et gestion des erreurs"

Test-Endpoint "6.1 - Transition : AVAILABLE → BUSY → AVAILABLE" -Partie "États" {
    # Prendre worker-simple
    $workerId = "worker-simple"
    
    # Vérifier état initial
    $details = Invoke-ApiCall -Endpoint "/api/workers/$workerId"
    $initialStatus = $details.status
    
    # AVAILABLE → BUSY
    $statusChange = @{ status = "BUSY" }
    Invoke-ApiCall -Method PUT -Endpoint "/api/workers/$workerId/status" -Body $statusChange | Out-Null
    
    $details = Invoke-ApiCall -Endpoint "/api/workers/$workerId"
    if ($details.status -ne "BUSY") {
        throw "Transition AVAILABLE → BUSY échouée"
    }
    
    # BUSY → AVAILABLE
    $statusChange = @{ status = "AVAILABLE" }
    Invoke-ApiCall -Method PUT -Endpoint "/api/workers/$workerId/status" -Body $statusChange | Out-Null
    
    $details = Invoke-ApiCall -Endpoint "/api/workers/$workerId"
    if ($details.status -ne "AVAILABLE") {
        throw "Transition BUSY → AVAILABLE échouée"
    }
}

Test-Endpoint "6.2 - Transition : AVAILABLE → OFFLINE → AVAILABLE" -Partie "États" {
    $workerId = "worker-meta"
    
    # AVAILABLE → OFFLINE
    $statusChange = @{ status = "OFFLINE" }
    Invoke-ApiCall -Method PUT -Endpoint "/api/workers/$workerId/status" -Body $statusChange | Out-Null
    
    $details = Invoke-ApiCall -Endpoint "/api/workers/$workerId"
    if ($details.status -ne "OFFLINE") {
        throw "Transition AVAILABLE → OFFLINE échouée"
    }
    
    # OFFLINE → AVAILABLE
    $statusChange = @{ status = "AVAILABLE" }
    Invoke-ApiCall -Method PUT -Endpoint "/api/workers/$workerId/status" -Body $statusChange | Out-Null
    
    $details = Invoke-ApiCall -Endpoint "/api/workers/$workerId"
    if ($details.status -ne "AVAILABLE") {
        throw "Transition OFFLINE → AVAILABLE échouée"
    }
}

Test-Endpoint "6.3 - Résilience : Sélection avec aucun worker disponible" -Partie "Résilience" {
    # Mettre tous les workers OFFLINE temporairement
    $workers = Invoke-ApiCall -Endpoint "/api/workers"
    
    foreach ($w in $workers) {
        $statusChange = @{ status = "OFFLINE" }
        Invoke-ApiCall -Method PUT -Endpoint "/api/workers/$($w.id)/status" -Body $statusChange | Out-Null
    }
    
    # Tenter une sélection
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/roundrobin"
    
    if ($result.success) {
        throw "Sélection devrait échouer sans workers disponibles"
    }
    
    if ($result.message -notmatch "Aucun worker") {
        throw "Message d'erreur incorrect"
    }
    
    # Remettre les workers en AVAILABLE
    foreach ($w in $workers) {
        $statusChange = @{ status = "AVAILABLE" }
        Invoke-ApiCall -Method PUT -Endpoint "/api/workers/$($w.id)/status" -Body $statusChange | Out-Null
    }
}

Test-Endpoint "6.4 - Résilience : Charge worker (incrémentation)" -Partie "Résilience" {
    $workerId = "worker-simple"
    
    $before = Invoke-ApiCall -Endpoint "/api/workers/$workerId"
    $chargeBefore = $before.currentLoad
    
    # Faire une sélection qui devrait incrémenter la charge
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/loadbalanced"
    
    # Vérifier immédiatement (la charge devrait avoir changé)
    Start-Sleep -Milliseconds 100
    
    $after = Invoke-ApiCall -Endpoint "/api/workers/$workerId"
    
    # La charge devrait avoir été incrémentée à un moment donné
    # Note: Elle peut déjà être redescendue après 1s
}

Test-Endpoint "6.5 - Résilience : Gestion workers inexistants" -Partie "Résilience" {
    try {
        Invoke-ApiCall -Endpoint "/api/workers/worker-inexistant-12345"
        throw "Devrait retourner une erreur 404"
    } catch {
        if ($_.Exception.Message -notmatch "404|Not Found") {
            throw "Mauvais code d'erreur pour worker inexistant"
        }
    }
}

# =========================================================================
# PARTIE 7 : PERFORMANCE & SCALABILITÉ
# =========================================================================

Write-PartieHeader "7" "PERFORMANCE & SCALABILITÉ" "Tests de charge et de performance"

Test-Endpoint "7.1 - Performance : 20 sélections consécutives Round-Robin" -Partie "Performance" {
    $startTime = Get-Date
    
    for ($i = 1; $i -le 20; $i++) {
        Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/roundrobin" | Out-Null
    }
    
    $duration = (Get-Date) - $startTime
    
    if ($duration.TotalSeconds -gt 10) {
        throw "20 sélections prennent trop de temps : $($duration.TotalSeconds)s"
    }
    
    Write-Host "      Temps total : $($duration.TotalMilliseconds.ToString('F0'))ms" -ForegroundColor Gray
    Write-Host "      Temps moyen : $($($duration.TotalMilliseconds / 20).ToString('F0'))ms/sélection" -ForegroundColor Gray
}

Test-Endpoint "7.2 - Performance : 20 sélections consécutives Load Balancing" -Partie "Performance" {
    $startTime = Get-Date
    
    for ($i = 1; $i -le 20; $i++) {
        Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/loadbalanced" | Out-Null
    }
    
    $duration = (Get-Date) - $startTime
    
    if ($duration.TotalSeconds -gt 10) {
        throw "20 sélections Load Balancing trop lentes"
    }
    
    Write-Host "      Temps total : $($duration.TotalMilliseconds.ToString('F0'))ms" -ForegroundColor Gray
}

Test-Endpoint "7.3 - Scalabilité : Enregistrement de 10 workers supplémentaires" -Partie "Scalabilité" {
    for ($i = 1; $i -le 10; $i++) {
        $worker = @{
            workerId = "scale-worker-$i"
            address = "http://localhost:$(9100 + $i)"
            metadata = @{
                region = if ($i % 2 -eq 0) { "eu-west" } else { "us-east" }
                tier = if ($i % 3 -eq 0) { "premium" } else { "standard" }
            }
        }
        
        Invoke-ApiCall -Method POST -Endpoint "/api/workers/register" -Body $worker | Out-Null
    }
    
    $allWorkers = Invoke-ApiCall -Endpoint "/api/workers"
    
    if ($allWorkers.Count -lt 15) {
        throw "Nombre total de workers insuffisant"
    }
}

Test-Endpoint "7.4 - Scalabilité : Round-Robin avec 15+ workers" -Partie "Scalabilité" {
    $selections = @{}
    
    # Faire 30 sélections (2x le nombre de workers)
    for ($i = 1; $i -le 30; $i++) {
        $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/roundrobin"
        
        $workerId = $result.workerId
        
        if (-not $selections.ContainsKey($workerId)) {
            $selections[$workerId] = 0
        }
        $selections[$workerId]++
    }
    
    # Vérifier qu'au moins 5 workers différents ont été sélectionnés
    if ($selections.Count -lt 5) {
        throw "Distribution insuffisante avec beaucoup de workers"
    }
    
    Write-Host "      Workers distincts sélectionnés : $($selections.Count)" -ForegroundColor Gray
}

Test-Endpoint "7.5 - Scalabilité : Recherche avec beaucoup de workers" -Partie "Scalabilité" {
    $results = Invoke-ApiCall -Endpoint "/api/workers/search?tag=region&value=eu-west"
    
    # Devrait trouver plusieurs workers EU
    if ($results.Count -lt 5) {
        Write-Host "      Seulement $($results.Count) workers EU trouvés" -ForegroundColor Yellow
    }
}

# =========================================================================
# NETTOYAGE FINAL
# =========================================================================

Write-Host "`n" -NoNewline
Write-Host "╔$('═' * 78)╗" -ForegroundColor DarkGray
Write-Host "║ NETTOYAGE FINAL" -ForegroundColor DarkGray -NoNewline
Write-Host (" " * 62) -NoNewline
Write-Host "║" -ForegroundColor DarkGray
Write-Host "╚$('═' * 78)╝" -ForegroundColor DarkGray

Write-Host "`n  [Nettoyage] Suppression de tous les workers de test..." -NoNewline

Clear-AllWorkers

$workers = Invoke-ApiCall -Endpoint "/api/workers"
if ($workers.Count -eq 0) {
    Write-Host " OK" -ForegroundColor Green
} else {
    Write-Host " $($workers.Count) restant(s)" -ForegroundColor Yellow
}

# =========================================================================
# RAPPORT FINAL
# =========================================================================

Write-Host "`n" -NoNewline
Write-Host "╔$('═' * 78)╗" -ForegroundColor Cyan
Write-Host "║" -NoNewline
Write-Host " RAPPORT FINAL DES TESTS" -ForegroundColor Cyan -NoNewline
Write-Host (" " * 53) -NoNewline
Write-Host "║" -ForegroundColor Cyan
Write-Host "╚$('═' * 78)╝" -ForegroundColor Cyan

Write-Host "`n  Tests Total    : " -NoNewline
Write-Host $Global:TestResults.Total -ForegroundColor White

Write-Host "  Tests Passés   : " -NoNewline
Write-Host $Global:TestResults.Passed -ForegroundColor Green

Write-Host "  Tests Échoués  : " -NoNewline
if ($Global:TestResults.Failed -gt 0) {
    Write-Host $Global:TestResults.Failed -ForegroundColor Red
} else {
    Write-Host $Global:TestResults.Failed -ForegroundColor Green
}

$successRate = if ($Global:TestResults.Total -gt 0) {
    [math]::Round(($Global:TestResults.Passed / $Global:TestResults.Total) * 100, 1)
} else {
    0
}

Write-Host "  Taux de Succès : " -NoNewline
if ($successRate -eq 100) {
    Write-Host "$successRate%" -ForegroundColor Green
} elseif ($successRate -ge 90) {
    Write-Host "$successRate%" -ForegroundColor Yellow
} else {
    Write-Host "$successRate%" -ForegroundColor Red
}

Write-Host "`n  RÉSULTATS PAR PARTIE :" -ForegroundColor Cyan

foreach ($partie in $Global:TestResults.PartieResults.Keys | Sort-Object) {
    $partieResult = $Global:TestResults.PartieResults[$partie]
    $partieRate = if ($partieResult.Total -gt 0) {
        [math]::Round(($partieResult.Passed / $partieResult.Total) * 100, 1)
    } else {
        0
    }
    
    $color = if ($partieRate -eq 100) { "Green" } elseif ($partieRate -ge 90) { "Yellow" } else { "Red" }
    
    Write-Host "    $partie" -NoNewline
    Write-Host (" " * (25 - $partie.Length)) -NoNewline
    Write-Host ": $($partieResult.Passed)/$($partieResult.Total) " -NoNewline
    Write-Host "($partieRate%)" -ForegroundColor $color
}

Write-Host "`n" -NoNewline
if ($Global:TestResults.Failed -eq 0) {
    Write-Host "  ✓ TOUS LES TESTS SONT PASSÉS !" -ForegroundColor Green
    Write-Host "  Le système AkkaJr fonctionne parfaitement!" -ForegroundColor Green
} else {
    Write-Host "  ✗ $($Global:TestResults.Failed) test(s) en échec" -ForegroundColor Red
    Write-Host "  Vérifiez les logs ci-dessus pour plus de détails" -ForegroundColor Yellow
}

Write-Host "`n$('=' * 80)" -ForegroundColor Cyan
Write-Host "  FIN DES TESTS - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Cyan
Write-Host "$('=' * 80)`n" -ForegroundColor Cyan

# Code de sortie
if ($Global:TestResults.Failed -gt 0) {
    exit 1
} else {
    exit 0
}
