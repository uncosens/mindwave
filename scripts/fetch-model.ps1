# Downloads what is not kept in git:
# - English: small Vosk model (about 40 MB) into app assets.
# - Korean: streaming zipformer for sherpa-onnx (kangkyu/icefall-asr-ko-streaming-zipformer-174m,
#   chunk 32, int8, about 160 MB), into app assets.
# - The sherpa-onnx Android library (about 50 MB) into app/libs.
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
$root = Split-Path $PSScriptRoot -Parent
$assets = Join-Path $root "app\src\main\assets"

$en = Join-Path $assets "model-en-us"
if (-not (Test-Path $en)) {
    $name = "vosk-model-small-en-us-0.15"
    $tmp = Join-Path $env:TEMP "$name.zip"
    Invoke-WebRequest "https://alphacephei.com/vosk/models/$name.zip" -OutFile $tmp
    Expand-Archive $tmp -DestinationPath $env:TEMP -Force
    New-Item -ItemType Directory -Force $assets | Out-Null
    Move-Item (Join-Path $env:TEMP $name) $en
    Remove-Item $tmp
    Write-Host "Model ready at $en"
}

$ko = Join-Path $assets "model-ko-sherpa"
if (-not (Test-Path $ko)) {
    $base = "https://huggingface.co/kangkyu/icefall-asr-ko-streaming-zipformer-174m/resolve/main"
    New-Item -ItemType Directory -Force $ko | Out-Null
    foreach ($part in "encoder", "decoder", "joiner") {
        Invoke-WebRequest "$base/$part-epoch-99-avg-1-chunk-32-left-128.int8.onnx" -OutFile (Join-Path $ko "$part.int8.onnx")
    }
    Invoke-WebRequest "$base/tokens.txt" -OutFile (Join-Path $ko "tokens.txt")
    Write-Host "Model ready at $ko"
}

$aar = Join-Path $root "app\libs\sherpa-onnx-2.0.1.aar"
if (-not (Test-Path $aar)) {
    New-Item -ItemType Directory -Force (Split-Path $aar) | Out-Null
    Invoke-WebRequest "https://github.com/k2-fsa/sherpa-onnx/releases/download/v2.0.1/sherpa-onnx-2.0.1.aar" -OutFile $aar
    Write-Host "Library ready at $aar"
}
