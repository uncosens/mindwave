# Downloads the small Vosk models into app assets: English (about 40 MB) and Korean (about 82 MB).
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$models = @{
    "model-en-us" = "vosk-model-small-en-us-0.15"
    "model-ko" = "vosk-model-small-ko-0.22"
}

foreach ($dir in $models.Keys) {
    $name = $models[$dir]
    $target = Join-Path $root "app\src\main\assets\$dir"
    if (Test-Path $target) { Write-Host "Already present: $target"; continue }
    $tmp = Join-Path $env:TEMP "$name.zip"
    Invoke-WebRequest "https://alphacephei.com/vosk/models/$name.zip" -OutFile $tmp
    Expand-Archive $tmp -DestinationPath $env:TEMP -Force
    New-Item -ItemType Directory -Force (Split-Path $target) | Out-Null
    Move-Item (Join-Path $env:TEMP $name) $target
    Remove-Item $tmp
    Write-Host "Model ready at $target"
}
