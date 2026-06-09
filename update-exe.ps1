param(
    [string]$OutputDir = "dist\KGM-eX-Employees-App",
    [switch]$CleanTarget
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSCommandPath
. (Join-Path $projectRoot "scripts\package-exe-common.ps1")

Invoke-KgmExeBuild `
    -ProjectRoot $projectRoot `
    -OutputDir $OutputDir `
    -CleanTarget:$CleanTarget `
    -PullLatest `
    -Activity "KGM Ex-Employees update"
