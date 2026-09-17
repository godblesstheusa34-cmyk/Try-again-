$ErrorActionPreference = 'Stop'
$TaskRoot = Split-Path $PSScriptRoot -Parent
$TaskVersion = '8.11.1'
$TaskHash = 'f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6'
$TaskCache = Join-Path $TaskRoot '.toolchain'
$TaskGradle = Join-Path $TaskCache "gradle-$TaskVersion/bin/gradle.bat"
if (-not (Test-Path $TaskGradle)) {
    New-Item -ItemType Directory -Force -Path $TaskCache | Out-Null
    $TaskZip = Join-Path $TaskCache "gradle-$TaskVersion-bin.zip"
    if (-not (Test-Path $TaskZip)) {
        Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$TaskVersion-bin.zip" -OutFile $TaskZip
    }
    if ((Get-FileHash $TaskZip -Algorithm SHA256).Hash.ToLowerInvariant() -ne $TaskHash) { throw 'Gradle checksum mismatch; delete the bad ZIP and retry.' }
    Expand-Archive -Path $TaskZip -DestinationPath $TaskCache -Force
}
Push-Location $TaskRoot
try { & $TaskGradle @args; $TaskExit = $LASTEXITCODE } finally { Pop-Location }
exit $TaskExit
