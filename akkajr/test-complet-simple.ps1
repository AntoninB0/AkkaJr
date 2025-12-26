# =========================================================================
# TESTS COMPLETS PAR PARTIES - AkkaJr
# =========================================================================

param(
    [string]$BaseUrl = "http://localhost:8080"
)

$Global:TestResults = @{
    Total = 0
    Passed = 0
    Failed = 0
}

function Test-Endpoint {
    param(
        [string]$Name,
        [scriptblock]$Test
    )
    
    $Global:TestResults.Total++
    
    Write-Host "`n  [TEST] $Name" -ForegroundColor Yellow
    
    try {
        & $Test
        $Global:TestResults.Passed++
        Write-Host "    [OK] PASSE" -ForegroundColor Green
        return $true
    } catch {
        $Global:TestResults.Failed++
        Write-Host "    [ERREUR] $_" -ForegroundColor Red
        return $false
    }
}

function Invoke-ApiCall {
    param(
        [string]$Method = "GET",
        [string]$Endpoint,
        [object]$Body = $null
    )
    
    $uri = "$BaseUrl$Endpoint"
    
    $params = @{
        Uri = $uri
        Method = $Method
    }
    
    if ($Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 10)
        $params.ContentType = "application/json"
    } elseif ($Method -eq "POST" -or $Method -eq "PUT") {
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
                Invoke-ApiCall -Method DELETE -Endpoint "/api/workers/$($worker.id)" | Out-Null
            } catch {}
        }
    } catch {}
}

# =========================================================================
# DEBUT DES TESTS
# =========================================================================

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "AKKAJR - TESTS COMPLETS PAR PARTIES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Nettoyer
Clear-AllWorkers

# =========================================================================
# PARTIE 1 : SYSTEME D'ACTEURS
# =========================================================================

Write-Host "`n=== PARTIE 1 : SYSTEME D'ACTEURS ===" -ForegroundColor Magenta

Test-Endpoint "1.1 - Creation d'acteur via API" {
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

Test-Endpoint "1.2 - Envoi de message asynchrone" {
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/roundrobin"
    
    if (-not $result.success) {
        throw "Message asynchrone echoue"
    }
}

Test-Endpoint "1.3 - Verification etat acteur" {
    $details = Invoke-ApiCall -Endpoint "/api/workers/actor-test-1"
    
    if ($details.status -ne "AVAILABLE") {
        throw "Statut acteur incorrect"
    }
}

Test-Endpoint "1.4 - Liste de tous les acteurs" {
    $workers = Invoke-ApiCall -Endpoint "/api/workers"
    
    if ($workers.Count -lt 1) {
        throw "Aucun acteur trouve"
    }
}

# =========================================================================
# PARTIE 2 : GESTION DES WORKERS (CRUD)
# =========================================================================

Write-Host "`n=== PARTIE 2 : GESTION DES WORKERS ===" -ForegroundColor Magenta

Test-Endpoint "2.1 - CREATE : Enregistrement worker simple" {
    $worker = @{
        workerId = "worker-simple"
        address = "http://localhost:9010"
        metadata = @{}
    }
    
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/workers/register" -Body $worker
    
    if ($result.workerId -ne "worker-simple") {
        throw "Enregistrement echoue"
    }
}

Test-Endpoint "2.2 - CREATE : Avec metadonnees" {
    $worker = @{
        workerId = "worker-meta"
        address = "http://localhost:9011"
        metadata = @{
            region = "eu-west-1"
            capability = "payment"
            tier = "premium"
        }
    }
    
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/workers/register" -Body $worker
    
    if ($result.status -ne "AVAILABLE") {
        throw "Statut initial incorrect"
    }
}

Test-Endpoint "2.3 - CREATE : Batch (3 workers)" {
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
        throw "Tous les workers ne sont pas enregistres"
    }
}

Test-Endpoint "2.4 - READ : Worker specifique" {
    $details = Invoke-ApiCall -Endpoint "/api/workers/worker-meta"
    
    if ($details.id -ne "worker-meta") {
        throw "Worker ID incorrect"
    }
}

Test-Endpoint "2.5 - READ : Liste tous" {
    $workers = Invoke-ApiCall -Endpoint "/api/workers"
    
    if ($workers.Count -lt 6) {
        throw "Nombre de workers incorrect"
    }
}

Test-Endpoint "2.6 - READ : Liste disponibles" {
    $available = Invoke-ApiCall -Endpoint "/api/workers/available"
    
    if ($available.Count -lt 1) {
        throw "Aucun worker disponible"
    }
}

Test-Endpoint "2.7 - UPDATE : Statut AVAILABLE -> BUSY" {
    $statusChange = @{ status = "BUSY" }
    
    Invoke-ApiCall -Method PUT -Endpoint "/api/workers/worker-simple/status" -Body $statusChange | Out-Null
    
    $details = Invoke-ApiCall -Endpoint "/api/workers/worker-simple"
    
    if ($details.status -ne "BUSY") {
        throw "Statut non mis a jour"
    }
}

Test-Endpoint "2.8 - UPDATE : Statut BUSY -> AVAILABLE" {
    $statusChange = @{ status = "AVAILABLE" }
    
    Invoke-ApiCall -Method PUT -Endpoint "/api/workers/worker-simple/status" -Body $statusChange | Out-Null
    
    $details = Invoke-ApiCall -Endpoint "/api/workers/worker-simple"
    
    if ($details.status -ne "AVAILABLE") {
        throw "Retour a AVAILABLE echoue"
    }
}

Test-Endpoint "2.9 - DELETE : Suppression worker" {
    Invoke-ApiCall -Method DELETE -Endpoint "/api/workers/actor-test-1"
    
    try {
        Invoke-ApiCall -Endpoint "/api/workers/actor-test-1"
        throw "Worker devrait etre supprime"
    } catch {
        if ($_.Exception.Message -notmatch "404|Not Found") {
            throw $_
        }
    }
}

# =========================================================================
# PARTIE 3 : HEALTH CHECKS & SUPERVISION
# =========================================================================

Write-Host "`n=== PARTIE 3 : HEALTH CHECKS ===" -ForegroundColor Magenta

Test-Endpoint "3.1 - Heartbeat : Enregistrement" {
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/workers/worker-meta/heartbeat"
    
    $details = Invoke-ApiCall -Endpoint "/api/workers/worker-meta"
    
    if (-not $details.lastHeartbeat) {
        throw "Heartbeat non enregistre"
    }
}

Test-Endpoint "3.2 - Health Check : Exclusion BUSY" {
    $statusChange = @{ status = "BUSY" }
    Invoke-ApiCall -Method PUT -Endpoint "/api/workers/batch-1/status" -Body $statusChange | Out-Null
    
    $available = Invoke-ApiCall -Endpoint "/api/workers/available"
    
    $busy = $available | Where-Object { $_.id -eq "batch-1" }
    
    if ($busy) {
        throw "Worker BUSY dans liste disponibles"
    }
    
    # Remettre en AVAILABLE
    $statusChange = @{ status = "AVAILABLE" }
    Invoke-ApiCall -Method PUT -Endpoint "/api/workers/batch-1/status" -Body $statusChange | Out-Null
}

Test-Endpoint "3.3 - Supervision : Charge worker" {
    $details = Invoke-ApiCall -Endpoint "/api/workers/worker-simple"
    
    if ($details.currentLoad -lt 0) {
        throw "Charge negative impossible"
    }
}

# =========================================================================
# PARTIE 4 : ROUTAGE DYNAMIQUE
# =========================================================================

Write-Host "`n=== PARTIE 4 : ROUTAGE DYNAMIQUE ===" -ForegroundColor Magenta

Test-Endpoint "4.1 - Round-Robin : Selection basique" {
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/roundrobin"
    
    if (-not $result.success) {
        throw "Selection Round-Robin echouee"
    }
}

Test-Endpoint "4.2 - Round-Robin : Distribution (6 selections)" {
    $selections = @{}
    
    for ($i = 1; $i -le 6; $i++) {
        $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/roundrobin"
        
        $workerId = $result.workerId
        
        if (-not $selections.ContainsKey($workerId)) {
            $selections[$workerId] = 0
        }
        $selections[$workerId]++
    }
    
    if ($selections.Count -lt 2) {
        throw "Distribution non equitable"
    }
}

Test-Endpoint "4.3 - Load Balancing : Selection basique" {
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/loadbalanced"
    
    if (-not $result.success) {
        throw "Selection Load Balancing echouee"
    }
}

Test-Endpoint "4.4 - Demo : Deux strategies" {
    $demo = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/demo"
    
    if (-not $demo.roundRobin.success) {
        throw "Round-Robin demo echoue"
    }
    
    if (-not $demo.loadBalanced.success) {
        throw "Load Balanced demo echoue"
    }
}

# =========================================================================
# PARTIE 5 : FILTRAGE & RECHERCHE
# =========================================================================

Write-Host "`n=== PARTIE 5 : FILTRAGE & RECHERCHE ===" -ForegroundColor Magenta

Test-Endpoint "5.1 - Recherche : Par tag region" {
    $results = Invoke-ApiCall -Endpoint "/api/workers/search?tag=region&value=us-east"
    
    if ($results.Count -lt 1) {
        throw "Aucun worker trouve pour region=us-east"
    }
}

Test-Endpoint "5.2 - Round-Robin avec filtre : region" {
    $filterRequest = @{
        filter = @{
            tag = "region"
            value = "us-east"
        }
    }
    
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/roundrobin" -Body $filterRequest
    
    if (-not $result.success) {
        throw "Selection avec filtre echouee"
    }
}

Test-Endpoint "5.3 - Load Balancing avec filtre : capability" {
    $filterRequest = @{
        filter = @{
            tag = "capability"
            value = "payment"
        }
    }
    
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/loadbalanced" -Body $filterRequest
    
    if (-not $result.success) {
        throw "Load balancing avec filtre echoue"
    }
}

# =========================================================================
# PARTIE 6 : ETATS & RESILIENCE
# =========================================================================

Write-Host "`n=== PARTIE 6 : ETATS & RESILIENCE ===" -ForegroundColor Magenta

Test-Endpoint "6.1 - Transition : AVAILABLE -> BUSY -> AVAILABLE" {
    $workerId = "worker-simple"
    
    # AVAILABLE -> BUSY
    $statusChange = @{ status = "BUSY" }
    Invoke-ApiCall -Method PUT -Endpoint "/api/workers/$workerId/status" -Body $statusChange | Out-Null
    
    $details = Invoke-ApiCall -Endpoint "/api/workers/$workerId"
    if ($details.status -ne "BUSY") {
        throw "Transition AVAILABLE -> BUSY echouee"
    }
    
    # BUSY -> AVAILABLE
    $statusChange = @{ status = "AVAILABLE" }
    Invoke-ApiCall -Method PUT -Endpoint "/api/workers/$workerId/status" -Body $statusChange | Out-Null
    
    $details = Invoke-ApiCall -Endpoint "/api/workers/$workerId"
    if ($details.status -ne "AVAILABLE") {
        throw "Transition BUSY -> AVAILABLE echouee"
    }
}

Test-Endpoint "6.2 - Resilience : Sans workers disponibles" {
    $workers = Invoke-ApiCall -Endpoint "/api/workers"
    
    foreach ($w in $workers) {
        $statusChange = @{ status = "OFFLINE" }
        Invoke-ApiCall -Method PUT -Endpoint "/api/workers/$($w.id)/status" -Body $statusChange | Out-Null
    }
    
    $result = Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/roundrobin"
    
    if ($result.success) {
        throw "Selection devrait echouer sans workers"
    }
    
    # Remettre en AVAILABLE
    foreach ($w in $workers) {
        $statusChange = @{ status = "AVAILABLE" }
        Invoke-ApiCall -Method PUT -Endpoint "/api/workers/$($w.id)/status" -Body $statusChange | Out-Null
    }
}

Test-Endpoint "6.3 - Resilience : Worker inexistant" {
    try {
        Invoke-ApiCall -Endpoint "/api/workers/worker-inexistant-12345"
        throw "Devrait retourner 404"
    } catch {
        if ($_.Exception.Message -notmatch "404|Not Found") {
            throw "Mauvais code d'erreur"
        }
    }
}

# =========================================================================
# PARTIE 7 : PERFORMANCE
# =========================================================================

Write-Host "`n=== PARTIE 7 : PERFORMANCE ===" -ForegroundColor Magenta

Test-Endpoint "7.1 - Performance : 10 selections Round-Robin" {
    $startTime = Get-Date
    
    for ($i = 1; $i -le 10; $i++) {
        Invoke-ApiCall -Method POST -Endpoint "/api/router/dynamic/roundrobin" | Out-Null
    }
    
    $duration = (Get-Date) - $startTime
    
    if ($duration.TotalSeconds -gt 5) {
        throw "10 selections trop lentes"
    }
    
    Write-Host "      Temps total : $($duration.TotalMilliseconds.ToString('F0'))ms" -ForegroundColor Gray
}

Test-Endpoint "7.2 - Scalabilite : 5 workers supplementaires" {
    for ($i = 1; $i -le 5; $i++) {
        $worker = @{
            workerId = "scale-worker-$i"
            address = "http://localhost:$(9100 + $i)"
            metadata = @{ region = "eu-west" }
        }
        
        Invoke-ApiCall -Method POST -Endpoint "/api/workers/register" -Body $worker | Out-Null
    }
    
    $allWorkers = Invoke-ApiCall -Endpoint "/api/workers"
    
    if ($allWorkers.Count -lt 10) {
        throw "Nombre total de workers insuffisant"
    }
}

# =========================================================================
# NETTOYAGE
# =========================================================================

Write-Host "`n=== NETTOYAGE ===" -ForegroundColor DarkGray

Clear-AllWorkers

$workers = Invoke-ApiCall -Endpoint "/api/workers"
Write-Host "  Workers restants : $($workers.Count)" -ForegroundColor Gray

# =========================================================================
# RAPPORT FINAL
# =========================================================================

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "RAPPORT FINAL DES TESTS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "`n  Tests Total    : $($Global:TestResults.Total)" -ForegroundColor White
Write-Host "  Tests Passes   : $($Global:TestResults.Passed)" -ForegroundColor Green
Write-Host "  Tests Echoues  : $($Global:TestResults.Failed)" -ForegroundColor $(if ($Global:TestResults.Failed -gt 0) { "Red" } else { "Green" })

$successRate = if ($Global:TestResults.Total -gt 0) {
    [math]::Round(($Global:TestResults.Passed / $Global:TestResults.Total) * 100, 1)
} else {
    0
}

Write-Host "  Taux de Succes : $successRate%" -ForegroundColor $(if ($successRate -eq 100) { "Green" } elseif ($successRate -ge 90) { "Yellow" } else { "Red" })

if ($Global:TestResults.Failed -eq 0) {
    Write-Host "`n  [OK] TOUS LES TESTS SONT PASSES !" -ForegroundColor Green
    Write-Host "  Le systeme AkkaJr fonctionne parfaitement!" -ForegroundColor Green
} else {
    Write-Host "`n  [ATTENTION] $($Global:TestResults.Failed) test(s) en echec" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Cyan

if ($Global:TestResults.Failed -gt 0) {
    exit 1
} else {
    exit 0
}
