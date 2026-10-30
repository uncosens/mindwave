# Downloads the small English Vosk model (about 40 MB) into app assets.
$ErrorActionPreference = "Stop"
$name = "vosk-model-small-en-us-0.15"
$root = Split-Path $PSScriptRoot -Parent
$target = Join-Path $root "app\src\main\assets\model-en-us"
$tmp = Join-Path $env:TEMP "$name.zip"

Invoke-WebRequest "https://alphacephei.com/vosk/models/$name.zip" -OutFile $tmp
Expand-Archive $tmp -DestinationPath $env:TEMP -Force
if (Test-Path $target) { Remove-Item $target -Recurse -Force }
New-Item -ItemType Directory -Force (Split-Path $target) | Out-Null
Move-Item (Join-Path $env:TEMP $name) $target
Remove-Item $tmp
Write-Host "Model ready at $target"
