# ============================================================================
# AkkaJr - Script de Test Automatise
# ============================================================================

$SERVICE1 = "http://localhost:8080"
$SERVICE2 = "http://localhost:8081"

# Couleurs
$ColorSuccess = "Green"
$ColorError = "Red"
$ColorInfo = "Cyan"
$ColorWarning = "Yellow"

# Compteurs
$TestsPassed = 0
$TestsFailed = 0

# ============================================================================
# FONCTIONS UTILITAIRES
# ============================================================================

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Url,
        [string]$Method = "GET",
        [string]$Body = $null,
        [int]$ExpectedStatus = 200
    )
    
    Write-Host "`n[$Name]" -ForegroundColor $ColorInfo
    Write-Host "  URL: $Url" -ForegroundColor Gray
    
    try {
        $headers = @{
            "Content-Type" = "application/json"
        }
        
        if ($Method -eq "GET") {
            $response = Invoke-WebRequest -Uri $Url -Method $Method -Headers $headers -TimeoutSec 10 -UseBasicParsing
        } else {
            $response = Invoke-WebRequest -Uri $Url -Method $Method -Headers $headers -Body $Body -TimeoutSec 10 -UseBasicParsing
        }
        
        if ($response.StatusCode -eq $ExpectedStatus) {
            Write-Host "  [OK] Status: $($response.StatusCode)" -ForegroundColor $ColorSuccess
            $script:TestsPassed++
            
            # Afficher le contenu JSON si present
            if ($response.Content) {
                try {
                    $json = $response.Content | ConvertFrom-Json
                    Write-Host "  Reponse: $($response.Content)" -ForegroundColor DarkGray
                } catch {
                    Write-Host "  Reponse: $($response.Content)" -ForegroundColor DarkGray
                }
            }
            return $true
        } else {
            Write-Host "  [ERREUR] Status: $($response.StatusCode)" -ForegroundColor $ColorError
            $script:TestsFailed++
            return $false
        }
    } catch {
        Write-Host "  [ERREUR] $($_.Exception.Message)" -ForegroundColor $ColorError
        $script:TestsFailed++
        return $false
    }
}

# ============================================================================
# TESTS
# ============================================================================

Write-Host "`n============================================================================" -ForegroundColor Cyan
Write-Host "  AKKAJR - TESTS AUTOMATISES" -ForegroundColor Cyan
Write-Host "============================================================================`n" -ForegroundColor Cyan

# ----------------------------------------------------------------------------
# 1. SANTE DU SERVICE
# ----------------------------------------------------------------------------

Write-Host "`n[PHASE 1] VERIFICATION DE LA SANTE DU SERVICE" -ForegroundColor Yellow
Write-Host "---------------------------------------------" -ForegroundColor Yellow

Test-Endpoint -Name "Health Check Service1" -Url "$SERVICE1/actuator/health"

Start-Sleep -Seconds 2

# ----------------------------------------------------------------------------
# 2. INITIALISATION DES ACTEURS
# ----------------------------------------------------------------------------

Write-Host "`n[PHASE 2] INITIALISATION DES ACTEURS" -ForegroundColor Yellow
Write-Host "-------------------------------------" -ForegroundColor Yellow

Test-Endpoint -Name "Init Acteurs" -Url "$SERVICE1/api/actors/init" -Method "POST"

Start-Sleep -Seconds 2

Test-Endpoint -Name "Liste Acteurs" -Url "$SERVICE1/api/actors/list"

Start-Sleep -Seconds 1

# ----------------------------------------------------------------------------
# 3. WORKFLOW COMMANDE
# ----------------------------------------------------------------------------

Write-Host "`n[PHASE 3] WORKFLOW DE COMMANDE" -ForegroundColor Yellow
Write-Host "-------------------------------" -ForegroundColor Yellow

$orderBody1 = '{"items":["Laptop","Mouse","Keyboard"]}'
Test-Endpoint -Name "Commande #1 (3 articles)" -Url "$SERVICE1/api/actors/order" -Method "POST" -Body $orderBody1

Start-Sleep -Seconds 3

$orderBody2 = '{"items":["Phone","Tablet"]}'
Test-Endpoint -Name "Commande #2 (2 articles)" -Url "$SERVICE1/api/actors/order" -Method "POST" -Body $orderBody2

Start-Sleep -Seconds 3

$orderBody3 = '{"items":["Monitor"]}'
Test-Endpoint -Name "Commande #3 (1 article)" -Url "$SERVICE1/api/actors/order" -Method "POST" -Body $orderBody3

Start-Sleep -Seconds 2

# ----------------------------------------------------------------------------
# 4. MESSAGING LOCAL
# ----------------------------------------------------------------------------

Write-Host "`n[PHASE 4] MESSAGING (TELL & ASK)" -ForegroundColor Yellow
Write-Host "---------------------------------" -ForegroundColor Yellow

$tellBody = '{"senderId":"agentA","receiverId":"agentB","content":"Hello from TELL"}'
Test-Endpoint -Name "Message TELL (fire-and-forget)" -Url "$SERVICE1/api/messages/tell" -Method "POST" -Body $tellBody

Start-Sleep -Seconds 1

$askBody = '{"senderId":"agentX","receiverId":"agentY","content":"Question from ASK"}'
Test-Endpoint -Name "Message ASK (avec reponse)" -Url "$SERVICE1/api/messages/ask" -Method "POST" -Body $askBody

Start-Sleep -Seconds 1

# ----------------------------------------------------------------------------
# 5. ROUTING
# ----------------------------------------------------------------------------

Write-Host "`n[PHASE 5] TESTS DE ROUTING" -ForegroundColor Yellow
Write-Host "---------------------------" -ForegroundColor Yellow

$roundRobinBody = '{"sender":"client","workers":["worker1","worker2","worker3"],"content":"Task A"}'
Test-Endpoint -Name "Round-Robin Router" -Url "$SERVICE1/api/router/roundrobin" -Method "POST" -Body $roundRobinBody

Start-Sleep -Seconds 2

$broadcastBody = '{"sender":"broadcaster","workers":["receiver1","receiver2","receiver3"],"content":"Broadcast Message"}'
Test-Endpoint -Name "Broadcast Router" -Url "$SERVICE1/api/router/broadcast" -Method "POST" -Body $broadcastBody

Start-Sleep -Seconds 2

# ----------------------------------------------------------------------------
# 6. METRIQUES ET MONITORING
# ----------------------------------------------------------------------------

Write-Host "`n[PHASE 6] METRIQUES ET MONITORING" -ForegroundColor Yellow
Write-Host "----------------------------------" -ForegroundColor Yellow

Test-Endpoint -Name "Metriques Acteurs" -Url "$SERVICE1/api/metrics/actors"

Start-Sleep -Seconds 1

Test-Endpoint -Name "Stats Messages" -Url "$SERVICE1/api/messages/stats"

Start-Sleep -Seconds 1

Test-Endpoint -Name "Prometheus Metrics" -Url "$SERVICE1/actuator/prometheus"

Start-Sleep -Seconds 1

# ----------------------------------------------------------------------------
# 7. HISTORIQUE
# ----------------------------------------------------------------------------

Write-Host "`n[PHASE 7] HISTORIQUE ET DEAD LETTERS" -ForegroundColor Yellow
Write-Host "-------------------------------------" -ForegroundColor Yellow

Test-Endpoint -Name "Historique Messages" -Url "$SERVICE1/api/messages/history"

Start-Sleep -Seconds 1

Test-Endpoint -Name "Dead Letters" -Url "$SERVICE1/api/messages/deadletters"

Start-Sleep -Seconds 1

# ----------------------------------------------------------------------------
# 8. EVENEMENTS RECENTS
# ----------------------------------------------------------------------------

Write-Host "`n[PHASE 8] EVENEMENTS RECENTS" -ForegroundColor Yellow
Write-Host "-----------------------------" -ForegroundColor Yellow

Test-Endpoint -Name "Evenements Recents" -Url "$SERVICE1/api/events/recent"

# ============================================================================
# RESUME
# ============================================================================

Write-Host "`n============================================================================" -ForegroundColor Cyan
Write-Host "  RESUME DES TESTS" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan

$Total = $TestsPassed + $TestsFailed
$SuccessRate = if ($Total -gt 0) { [math]::Round(($TestsPassed / $Total) * 100, 2) } else { 0 }

Write-Host "`n  Tests reussis    : $TestsPassed" -ForegroundColor Green
Write-Host "  Tests echoues    : $TestsFailed" -ForegroundColor Red
Write-Host "  Total            : $Total"
Write-Host "  Taux de reussite : $SuccessRate%" -ForegroundColor $(if ($SuccessRate -eq 100) { "Green" } else { "Yellow" })

Write-Host "`n============================================================================" -ForegroundColor Cyan
Write-Host "  DASHBOARDS DISPONIBLES" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan

Write-Host "`n  Observabilite : http://localhost:8080/observability" -ForegroundColor White
Write-Host "  Communication : http://localhost:8080/dashboard" -ForegroundColor White
Write-Host "  Prometheus    : http://localhost:8080/actuator/prometheus" -ForegroundColor White

Write-Host "`n============================================================================" -ForegroundColor Cyan
Write-Host "  COMMANDES UTILES" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan

Write-Host "`n  Logs Docker       : docker logs -f akkajr-service1" -ForegroundColor Gray
Write-Host "  Arreter           : docker stop akkajr-service1" -ForegroundColor Gray
Write-Host "  Supprimer         : docker rm akkajr-service1" -ForegroundColor Gray
Write-Host "  Rebuild           : docker build -t akkajr ." -ForegroundColor Gray
Write-Host "  Docker Compose    : docker-compose up -d --build" -ForegroundColor Gray
Write-Host "  Arreter Compose   : docker-compose down" -ForegroundColor Gray

Write-Host "`n============================================================================`n" -ForegroundColor Cyan

# Ouvrir les dashboards dans le navigateur (optionnel)
$openBrowser = Read-Host "`nVoulez-vous ouvrir les dashboards dans le navigateur? (O/N)"
if ($openBrowser -eq "O" -or $openBrowser -eq "o") {
    Start-Process "http://localhost:8080/observability"
    Start-Process "http://localhost:8080/dashboard"
    Write-Host "`nDashboards ouverts dans le navigateur!" -ForegroundColor Green
}

Write-Host "`nAppuyez sur une touche pour quitter..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")