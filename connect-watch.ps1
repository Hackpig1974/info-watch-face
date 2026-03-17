$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

Write-Host "Checking connected devices..."
& $adb devices

Write-Host ""
$ip = Read-Host "Enter watch IP (or press Enter for 192.168.10.164)"
if ($ip -eq "") { $ip = "192.168.10.164" }

$port = Read-Host "Enter watch port (shown in Wireless Debugging screen)"

$target = "${ip}:${port}"
Write-Host "Connecting to $target..."
& $adb connect $target

Write-Host ""
Write-Host "Connected devices:"
& $adb devices
