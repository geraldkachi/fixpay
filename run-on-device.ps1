# FixPay Mobile PWA Run-On-Device Script
# This script starts both the Laravel backend and React PWA servers, exposing them to the local network for testing on mobile.

# 1. Determine Local IP Address
$localIp = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { 
    $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*" 
} | Select-Object -First 1).IPAddress

if (-not $localIp) {
    $localIp = "YOUR_PC_IP"
}

Clear-Host
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "                FIXPAY MOBILE RUN-ON-DEVICE SCRIPT               " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  1. Connect your mobile device and PC to the SAME Wi-Fi network." -ForegroundColor Yellow
Write-Host "  2. Open the following URL in your mobile browser:" -ForegroundColor Yellow
Write-Host "     --> http://$localIp:5273" -ForegroundColor Green -BackgroundColor Black
Write-Host "  3. In your mobile browser, tap 'Add to Home Screen' to install." -ForegroundColor Yellow
Write-Host ""
Write-Host "  Starting servers now... (Ctrl+C to terminate both)" -ForegroundColor Gray
Write-Host "=================================================================" -ForegroundColor Cyan

# 2. Concurrently Run Backend and PWA
$backendPath = Join-Path $PSScriptRoot "fixpay-laravel"
$pwaPath = Join-Path $PSScriptRoot "fixpay-pwa"

# Start Laravel Backend on Port 8081
$phpPath = "C:\Users\kolugbenga\AppData\Local\Microsoft\WinGet\Packages\PHP.PHP.8.3_Microsoft.Winget.Source_8wekyb3d8bbwe\php.exe"
$backendJob = Start-Process $phpPath -ArgumentList "artisan serve --port=8081" -WorkingDirectory $backendPath -NoNewWindow -PassThru

# Start Vite PWA on Port 5273 (host=true already configured in vite.config.ts)
$pwaJob = Start-Process npm -ArgumentList "run dev" -WorkingDirectory $pwaPath -NoNewWindow -PassThru

# Monitor process execution and wait for Ctrl+C
try {
    while ($true) {
        Start-Sleep -Seconds 1
    }
}
finally {
    Write-Host ""
    Write-Host "Stopping servers..." -ForegroundColor Red
    Stop-Process -Id $backendJob.Id -Force -ErrorAction SilentlyContinue
    Stop-Process -Id $pwaJob.Id -Force -ErrorAction SilentlyContinue
    Write-Host "Servers stopped successfully." -ForegroundColor Green
}
