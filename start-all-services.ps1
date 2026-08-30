$BASE_PATH = "D:\cloud-native-book-commerce"

$services = @(
    "user-service",
    "product-service",
    "inventory-service",
    "cart-service",
    "order-service",
    "payment-service",
    "notification-service"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Starting Book Commerce Microservices" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Start 7 microservices
foreach ($service in $services) {

    $servicePath = Join-Path $BASE_PATH $service

    Write-Host ""
    Write-Host "Starting $service ..." -ForegroundColor Yellow

    Start-Process powershell.exe `
        -ArgumentList "-NoExit", "-Command", "Set-Location '$servicePath'; mvn spring-boot:run" `
        -WindowStyle Normal
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "7 microservices started." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# Give services some time to start
Write-Host ""
Write-Host "Waiting 10 seconds before starting API Gateway..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Start API Gateway
$gatewayPath = Join-Path $BASE_PATH "api-gateway"

Write-Host ""
Write-Host "Starting API Gateway..." -ForegroundColor Cyan

Start-Process powershell.exe `
    -ArgumentList "-NoExit", "-Command", "Set-Location '$gatewayPath'; mvn spring-boot:run" `
    -WindowStyle Normal

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host " API Gateway started on port 8080" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green