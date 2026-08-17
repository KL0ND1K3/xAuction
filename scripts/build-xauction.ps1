$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$pluginDir = Join-Path $root "xAuction-src"
$distDir = Join-Path $root "xAuction-plugin"
$pluginsDir = Join-Path $root "server\plugins"

& (Join-Path $pluginDir "mvn.ps1") -q -DskipTests package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

New-Item -ItemType Directory -Force -Path $pluginsDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $distDir "xAuction") | Out-Null

Get-ChildItem (Join-Path $pluginDir "target") -Filter "xAuction-*.jar" |
    Where-Object { $_.Name -notlike "original-*" } |
    ForEach-Object {
        Copy-Item $_.FullName (Join-Path $pluginsDir $_.Name) -Force
        Copy-Item $_.FullName (Join-Path $distDir $_.Name) -Force
        Write-Host "Copied $($_.Name) -> server\plugins"
        Write-Host "Copied $($_.Name) -> xAuction-plugin"
    }

$res = Join-Path $pluginDir "src\main\resources"
Copy-Item (Join-Path $res "config.yml") (Join-Path $distDir "xAuction\config.yml") -Force
Copy-Item (Join-Path $res "gui.yml") (Join-Path $distDir "xAuction\gui.yml") -Force
Copy-Item (Join-Path $res "messages.yml") (Join-Path $distDir "xAuction\messages.yml") -Force
