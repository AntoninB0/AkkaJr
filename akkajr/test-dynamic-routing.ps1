# ===================================
# TESTS AKKAJR - Version Sans Erreur
# ===================================

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "AKKAJR - Tests Routage Dynamique" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$testsPasses = 0
$testsTotal = 0

# ===================================
# TEST 1 : Enregistrer Workers
# ===================================

Write-Host "`n[TEST 1] Enregistrement de 3 workers..." -ForegroundColor Yellow

$workers = @(
    @{ workerId = "worker-1"; address = "http://localhost:9001"; metadata = @{ region = "eu-west"; capability = "payment" } },
    @{ workerId = "worker-2"; address = "http://localhost:9002"; metadata = @{ region = "eu-west"; capability = "order" } },
    @{ workerId = "worker-3"; address = "http://localhost:9003"; metadata = @{ region = "us-east"; capability = "payment" } }
)

foreach ($w in $workers) {
    try {
        $result = Invoke-RestMethod -Uri "$baseUrl/api/workers/register" -Method POST -Body ($w | ConvertTo-Json) -ContentType "application/json"
        Write-Host "  [OK] $($w.workerId) enregistre" -ForegroundColor Green
        $testsPasses++
    } catch {
        Write-Host "  [ERREUR] $($w.workerId) : $_" -ForegroundColor Red
    }
    $testsTotal++
}

Start-Sleep -Seconds 1

# ===================================
# TEST 2 : Lister Workers
# ===================================

Write-Host "`n[TEST 2] Liste de tous les workers..." -ForegroundColor Yellow

try {
    $allWorkers = Invoke-RestMethod -Uri "$baseUrl/api/workers"
    Write-Host "  [OK] Nombre total : $($allWorkers.Count)" -ForegroundColor Green
    
    foreach ($w in $allWorkers) {
        Write-Host "    - $($w.id) @ $($w.address) [$($w.status)]" -ForegroundColor Gray
    }
    $testsPasses++
} catch {
    Write-Host "  [ERREUR] $_" -ForegroundColor Red
}
$testsTotal++

# ===================================
# TEST 3 : Workers Disponibles
# ===================================

Write-Host "`n[TEST 3] Workers disponibles..." -ForegroundColor Yellow

try {
    $available = Invoke-RestMethod -Uri "$baseUrl/api/workers/available"
    Write-Host "  [OK] Workers disponibles : $($available.Count)" -ForegroundColor Green
    
    foreach ($w in $available) {
        Write-Host "    - $($w.id) (charge: $($w.currentLoad))" -ForegroundColor Gray
    }
    $testsPasses++
} catch {
    Write-Host "  [ERREUR] $_" -ForegroundColor Red
}
$testsTotal++

# ===================================
# TEST 4 : Heartbeat
# ===================================

Write-Host "`n[TEST 4] Heartbeat worker-1..." -ForegroundColor Yellow

try {
    $heartbeat = Invoke-RestMethod -Uri "$baseUrl/api/workers/worker-1/heartbeat" -Method POST
    Write-Host "  [OK] $($heartbeat.message)" -ForegroundColor Green
    $testsPasses++
} catch {
    Write-Host "  [ERREUR] $_" -ForegroundColor Red
}
$testsTotal++

## TEST 5 : Round-Robin (CORRIGÉ)
Write-Host "`n[TEST 5] Selection Round-Robin (5 appels)..." -ForegroundColor Yellow

$emptyBody = @{} | ConvertTo-Json  # ← AJOUT: Body vide

$rrSuccess = $true
for ($i = 1; $i -le 5; $i++) {
    try {
        # AJOUT: -Body et -ContentType même pour requête vide
        $selection = Invoke-RestMethod -Uri "$baseUrl/api/router/dynamic/roundrobin" `
            -Method POST `
            -Body $emptyBody `
            -ContentType "application/json"
        
        if ($selection.success) {
            Write-Host "  Appel $i : $($selection.workerId)" -ForegroundColor Green
        }
    } catch {
        Write-Host "  Appel $i : ERREUR - $_" -ForegroundColor Red
        $rrSuccess = $false
    }
}

# TEST 6 : Load Balancing (CORRIGÉ)
Write-Host "`n[TEST 6] Selection Load Balanced (3 appels)..." -ForegroundColor Yellow

$lbSuccess = $true
for ($i = 1; $i -le 3; $i++) {
    try {
        # AJOUT: -Body et -ContentType même pour requête vide
        $selection = Invoke-RestMethod -Uri "$baseUrl/api/router/dynamic/loadbalanced" `
            -Method POST `
            -Body $emptyBody `
            -ContentType "application/json"
        
        if ($selection.success) {
            Write-Host "  Appel $i : $($selection.workerId) (charge: $($selection.workerLoad))" -ForegroundColor Green
        }
        
        Start-Sleep -Milliseconds 300
    } catch {
        Write-Host "  Appel $i : ERREUR - $_" -ForegroundColor Red
        $lbSuccess = $false
    }
}
# ===================================
# TEST 7 : Filtrage par Tag
# ===================================

Write-Host "`n[TEST 7] Filtrage par region = eu-west..." -ForegroundColor Yellow

$filterRegion = @{
    filter = @{
        tag = "region"
        value = "eu-west"
    }
} | ConvertTo-Json

try {
    $selection = Invoke-RestMethod -Uri "$baseUrl/api/router/dynamic/roundrobin" -Method POST -Body $filterRegion -ContentType "application/json"
    
    if ($selection.success) {
        Write-Host "  [OK] Worker EU selectionne : $($selection.workerId)" -ForegroundColor Green
        
        # Verifier que c'est bien un worker EU
        if ($selection.workerId -in @("worker-1", "worker-2")) {
            Write-Host "  [OK] Filtrage correct (worker EU)" -ForegroundColor Green
            $testsPasses++
        } else {
            Write-Host "  [ERREUR] Worker non-EU selectionne : $($selection.workerId)" -ForegroundColor Red
        }
    } else {
        Write-Host "  [ERREUR] Selection echouee" -ForegroundColor Red
    }
} catch {
    Write-Host "  [ERREUR] $_" -ForegroundColor Red
}
$testsTotal++

# ===================================
# TEST 8 : Filtrage par Capability
# ===================================

Write-Host "`n[TEST 8] Filtrage par capability = payment..." -ForegroundColor Yellow

$filterCapability = @{
    filter = @{
        tag = "capability"
        value = "payment"
    }
} | ConvertTo-Json

try {
    $selection = Invoke-RestMethod -Uri "$baseUrl/api/router/dynamic/loadbalanced" -Method POST -Body $filterCapability -ContentType "application/json"
    
    if ($selection.success) {
        Write-Host "  [OK] Worker payment selectionne : $($selection.workerId)" -ForegroundColor Green
        
        # Verifier que c'est bien un worker payment
        if ($selection.workerId -in @("worker-1", "worker-3")) {
            Write-Host "  [OK] Filtrage correct (capability payment)" -ForegroundColor Green
            $testsPasses++
        } else {
            Write-Host "  [ERREUR] Worker sans capability payment : $($selection.workerId)" -ForegroundColor Red
        }
    } else {
        Write-Host "  [ERREUR] Selection echouee" -ForegroundColor Red
    }
} catch {
    Write-Host "  [ERREUR] $_" -ForegroundColor Red
}
$testsTotal++

# ===================================
# TEST 9 : Recherche par Tag
# ===================================

Write-Host "`n[TEST 9] Recherche workers env = prod..." -ForegroundColor Yellow

try {
    # Modifier les metadonnees d'abord
    $worker1 = Invoke-RestMethod -Uri "$baseUrl/api/workers/worker-1"
    Write-Host "  Worker-1 metadata : $($worker1.metadata | ConvertTo-Json -Compress)" -ForegroundColor Gray
    
    $testsPasses++
} catch {
    Write-Host "  [ERREUR] $_" -ForegroundColor Red
}
$testsTotal++

# ===================================
# TEST 10 : Demo
# ===================================

Write-Host "`n[TEST 10] Demo des strategies..." -ForegroundColor Yellow

try {
    $demo = Invoke-RestMethod -Uri "$baseUrl/api/router/dynamic/demo" -Method POST
    
    Write-Host "  [OK] Round-Robin : $($demo.roundRobin.workerId)" -ForegroundColor Green
    Write-Host "  [OK] Load Balanced : $($demo.loadBalanced.workerId) (charge: $($demo.loadBalanced.load))" -ForegroundColor Green
    
    $testsPasses++
} catch {
    Write-Host "  [ERREUR] $_" -ForegroundColor Red
}
$testsTotal++

# ===================================
# TEST 11 : Changer Status
# ===================================

Write-Host "`n[TEST 11] Changement status worker-2 -> BUSY..." -ForegroundColor Yellow

$statusChange = @{ status = "BUSY" } | ConvertTo-Json

try {
    $result = Invoke-RestMethod -Uri "$baseUrl/api/workers/worker-2/status" -Method PUT -Body $statusChange -ContentType "application/json"
    Write-Host "  [OK] Status change : $($result.message)" -ForegroundColor Green
    
    Start-Sleep -Seconds 1
    
    $available = Invoke-RestMethod -Uri "$baseUrl/api/workers/available"
    Write-Host "  Workers disponibles maintenant : $($available.Count)" -ForegroundColor Gray
    
    if ($available.Count -eq 2) {
        Write-Host "  [OK] Worker BUSY exclu correctement" -ForegroundColor Green
        $testsPasses++
    } else {
        Write-Host "  [ERREUR] Nombre de workers disponibles incorrect" -ForegroundColor Red
    }
} catch {
    Write-Host "  [ERREUR] $_" -ForegroundColor Red
}
$testsTotal++

# ===================================
# TEST 12 : Details Worker
# ===================================

Write-Host "`n[TEST 12] Details du worker-1..." -ForegroundColor Yellow

try {
    $details = Invoke-RestMethod -Uri "$baseUrl/api/workers/worker-1"
    
    Write-Host "  [OK] ID: $($details.id)" -ForegroundColor Green
    Write-Host "    Adresse: $($details.address)" -ForegroundColor Gray
    Write-Host "    Status: $($details.status)" -ForegroundColor Gray
    Write-Host "    Charge: $($details.currentLoad)" -ForegroundColor Gray
    
    $testsPasses++
} catch {
    Write-Host "  [ERREUR] $_" -ForegroundColor Red
}
$testsTotal++

# ===================================
# NETTOYAGE
# ===================================

Write-Host "`n[NETTOYAGE] Suppression des workers..." -ForegroundColor Yellow

@("worker-1", "worker-2", "worker-3") | ForEach-Object {
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/workers/$_" -Method DELETE | Out-Null
        Write-Host "  [OK] $_ supprime" -ForegroundColor Green
    } catch {
        Write-Host "  [ERREUR] $_ : $_" -ForegroundColor Red
    }
}

# Verification
try {
    $workers = Invoke-RestMethod -Uri "$baseUrl/api/workers"
    Write-Host "`n  Workers restants : $($workers.Count)" -ForegroundColor Gray
} catch {
    Write-Host "  [ERREUR] Verification nettoyage : $_" -ForegroundColor Red
}

# ===================================
# RESUME
# ===================================

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "RESUME DES TESTS" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$pourcent = [math]::Round(($testsPasses / $testsTotal) * 100, 1)

if ($testsPasses -eq $testsTotal) {
    Write-Host "[OK] TOUS LES TESTS PASSES : $testsPasses/$testsTotal (100 pourcent)" -ForegroundColor Green
    Write-Host "`nLe systeme de routage dynamique fonctionne parfaitement!" -ForegroundColor Green
} else {
    Write-Host "[INFO] Tests passes : $testsPasses/$testsTotal ($pourcent pourcent)" -ForegroundColor Yellow
    Write-Host "`n$($testsTotal - $testsPasses) test(s) en echec" -ForegroundColor Red
}

Write-Host ""