$ErrorActionPreference = "Stop"

$script:KgmStepIndex = 0
$script:KgmTotalSteps = 12
$script:KgmActivity = "KGM Ex-Employees packaging"
$script:KgmAppName = "KGM Ex Employees"
$script:KgmExeName = "KGM Ex Employees.exe"
$script:KgmJarName = "kgm-ex-employee-management-1.0.0.jar"
$script:KgmMainClass = "com.kgm.Main"

function Write-KgmStep {
    param(
        [string]$Message,
        [int]$Percent
    )

    $script:KgmStepIndex++
    $status = "Step $script:KgmStepIndex of $script:KgmTotalSteps - $Message"
    Write-Progress -Activity $script:KgmActivity -Status $status -PercentComplete $Percent
    Write-Host ("[{0,3}%] {1}" -f $Percent, $Message)
}

function Complete-KgmProgress {
    Write-Progress -Activity $script:KgmActivity -Completed
}

function Require-KgmCommand {
    param(
        [string]$Name,
        [string]$Help
    )

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $command) {
        throw "Required tool '$Name' was not found. $Help"
    }
    Write-Host "  OK: $Name -> $($command.Source)"
}

function Check-KgmRequiredTools {
    Require-KgmCommand -Name "java" -Help "Install JDK 21 or newer and add it to PATH."
    Require-KgmCommand -Name "mvn" -Help "Install Apache Maven and add it to PATH."
    Require-KgmCommand -Name "jpackage" -Help "Install a full JDK 21 or newer; jpackage is included with the JDK."
}

function Check-KgmGit {
    Require-KgmCommand -Name "git" -Help "Install Git and add it to PATH."
}

function Require-KgmJava21 {
    $versionOutput = & java --version
    if ($LASTEXITCODE -ne 0 -or -not $versionOutput) {
        throw "Could not determine Java version. Install JDK 21 or newer."
    }

    $firstLine = ($versionOutput | Select-Object -First 1).ToString()
    $versionText = ($firstLine -replace "^[^\d]*", "").Trim()
    $majorText = ($versionText -split "\.")[0]
    $major = [int]$majorText
    if ($major -lt 21) {
        throw "Java $versionText found. This project requires JDK 21 or newer."
    }

    Write-Host "  OK: Java $versionText"
}

function Check-KgmMaven {
    $versionOutput = & mvn --version
    if ($LASTEXITCODE -ne 0 -or -not $versionOutput) {
        throw "Could not run Maven. Install Maven and add mvn to PATH."
    }
    $firstLine = ($versionOutput | Select-Object -First 1).ToString()
    Write-Host "  OK: $firstLine"
}

function Check-KgmJPackage {
    $versionOutput = & jpackage --version
    if ($LASTEXITCODE -ne 0 -or -not $versionOutput) {
        throw "Could not run jpackage. Install a full JDK 21 or newer."
    }
    $firstLine = ($versionOutput | Select-Object -First 1).ToString()
    Write-Host "  OK: jpackage $firstLine"
}

function Get-KgmEnvFileValue {
    param(
        [string]$Path,
        [string]$Name,
        [string]$DefaultValue
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $DefaultValue
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $text = $line.Trim()
        if ($text.Length -eq 0 -or $text.StartsWith("#")) {
            continue
        }
        if ($text.StartsWith("export ")) {
            $text = $text.Substring(7).Trim()
        }
        $separator = $text.IndexOf("=")
        if ($separator -le 0) {
            continue
        }
        $key = $text.Substring(0, $separator).Trim()
        if ($key -ne $Name) {
            continue
        }
        $value = $text.Substring($separator + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        return $value
    }

    return $DefaultValue
}

function Get-KgmEnvFile {
    param(
        [string]$ProjectRoot,
        [string]$OutputPath
    )

    $projectEnv = Join-Path $ProjectRoot ".env"
    $outputEnv = Join-Path $OutputPath "config\.env"
    if (Test-Path -LiteralPath $projectEnv -PathType Leaf) {
        return $projectEnv
    }
    if (Test-Path -LiteralPath $outputEnv -PathType Leaf) {
        return $outputEnv
    }
    return $null
}

function Test-KgmMySqlPort {
    param(
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutMs = 3000
    )

    $client = $null
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $async = $client.BeginConnect($HostName, $Port, $null, $null)
        $success = $async.AsyncWaitHandle.WaitOne($TimeoutMs, $false)
        if (-not $success) {
            $client.Close()
            Write-Warning "MySQL check timed out after $TimeoutMs ms. The build can continue; the app will show setup instructions at runtime."
            return $false
        }
        $client.EndConnect($async)
        $client.Close()
        Write-Host "  OK: MySQL reachable at ${HostName}:${Port}"
        return $true
    } catch {
        if ($client) {
            $client.Close()
        }
        Write-Warning "MySQL not reachable at ${HostName}:${Port}. The build can continue; the app will show setup instructions at runtime."
        return $false
    }
}

function Check-KgmMySqlReachable {
    param([string]$EnvFile)

    if (-not $EnvFile) {
        Write-Warning ".env was not found. Continuing build; create .env or config\.env before running the app."
        return
    }

    $hostName = Get-KgmEnvFileValue -Path $EnvFile -Name "KGM_DB_HOST" -DefaultValue "127.0.0.1"
    $portText = Get-KgmEnvFileValue -Path $EnvFile -Name "KGM_DB_PORT" -DefaultValue "3306"
    $port = [int]$portText
    Test-KgmMySqlPort -HostName $hostName -Port $port -TimeoutMs 3000 | Out-Null
}

function ConvertTo-KgmArgumentString {
    param([string[]]$Arguments)

    $quoted = foreach ($argument in $Arguments) {
        if ($null -eq $argument) {
            '""'
        } elseif ($argument -match '[\s"]') {
            '"' + ($argument -replace '"', '\"') + '"'
        } else {
            $argument
        }
    }

    return ($quoted -join " ")
}

function Invoke-KgmExternalCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments = @(),
        [string]$Description = $FilePath
    )

    $command = Get-Command $FilePath -ErrorAction SilentlyContinue
    if (-not $command) {
        throw "Command not found: $FilePath"
    }

    Write-Host "  Running: $Description"
    Write-Host "  $FilePath $(ConvertTo-KgmArgumentString -Arguments $Arguments)"
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed with exit code $LASTEXITCODE."
    }
}

function Clear-KgmTargetFolder {
    param(
        [string]$ProjectRoot,
        [string]$OutputPath
    )

    $targetPath = Join-Path $ProjectRoot "target"
    $resolvedTarget = [System.IO.Path]::GetFullPath($targetPath)
    $resolvedOutput = [System.IO.Path]::GetFullPath($OutputPath)
    $projectRootFull = [System.IO.Path]::GetFullPath($ProjectRoot)
    $projectPrefix = $projectRootFull.TrimEnd('\') + '\'

    if (-not $resolvedTarget.StartsWith($projectPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to clean target outside workspace: $resolvedTarget"
    }
    if ($resolvedOutput.StartsWith($resolvedTarget.TrimEnd('\') + '\', [System.StringComparison]::OrdinalIgnoreCase)) {
        Write-Warning "OutputDir is inside target, so full target cleanup is skipped to preserve packaged app data."
        return
    }
    if (Test-Path -LiteralPath $resolvedTarget) {
        Remove-Item -LiteralPath $resolvedTarget -Recurse -Force
    }
}

function Copy-KgmDirectoryFresh {
    param(
        [string]$Source,
        [string]$Destination
    )

    if (Test-Path -LiteralPath $Destination) {
        Remove-Item -LiteralPath $Destination -Recurse -Force
    }
    Copy-Item -LiteralPath $Source -Destination $Destination -Recurse -Force
}

function Copy-KgmDirectoryIfMissing {
    param(
        [string]$Source,
        [string]$Destination
    )

    if (Test-Path -LiteralPath $Destination -PathType Container) {
        Write-Host "  Kept existing: $Destination"
        return
    }
    if (Test-Path -LiteralPath $Source -PathType Container) {
        Copy-Item -LiteralPath $Source -Destination $Destination -Recurse -Force
    } else {
        New-Item -ItemType Directory -Path $Destination -Force | Out-Null
    }
}

function Backup-KgmAppFiles {
    param(
        [string]$OutputPath,
        [string]$Label
    )

    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $backupDir = Join-Path $OutputPath "backups\$Label-$timestamp"
    New-Item -ItemType Directory -Path $backupDir -Force | Out-Null

    $items = @($script:KgmExeName, "app", "runtime")
    $hasBackup = $false
    foreach ($item in $items) {
        $path = Join-Path $OutputPath $item
        if (Test-Path -LiteralPath $path) {
            Copy-Item -LiteralPath $path -Destination $backupDir -Recurse -Force
            $hasBackup = $true
        }
    }

    if ($hasBackup) {
        Write-Host "  Backup created: $backupDir"
    } else {
        Write-Host "  No existing app files found to back up."
    }
}

function Ensure-KgmProtectedFolders {
    param(
        [string]$ProjectRoot,
        [string]$OutputPath
    )

    New-Item -ItemType Directory -Path $OutputPath -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $OutputPath "backups") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $OutputPath "config") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $OutputPath "logs") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $OutputPath "employees") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $OutputPath "resources\employees") -Force | Out-Null
    $outputImages = Join-Path $OutputPath "images"
    New-Item -ItemType Directory -Path (Join-Path $outputImages "uploads") -Force | Out-Null
    $sourceImages = Join-Path $ProjectRoot "images"
    if (Test-Path -LiteralPath $sourceImages -PathType Container) {
        foreach ($image in Get-ChildItem -LiteralPath $sourceImages -File) {
            $destinationImage = Join-Path $outputImages $image.Name
            if (-not (Test-Path -LiteralPath $destinationImage -PathType Leaf)) {
                Copy-Item -LiteralPath $image.FullName -Destination $destinationImage -Force
            }
        }
    }
}

function Copy-KgmEnvFiles {
    param(
        [string]$ProjectRoot,
        [string]$OutputPath
    )

    $projectEnv = Join-Path $ProjectRoot ".env"
    $exampleEnv = Join-Path $ProjectRoot ".env.example"
    $targetEnv = Join-Path $OutputPath "config\.env"
    $targetExample = Join-Path $OutputPath "config\.env.example"

    if (-not (Test-Path -LiteralPath $targetEnv -PathType Leaf)) {
        if (Test-Path -LiteralPath $projectEnv -PathType Leaf) {
            Copy-Item -LiteralPath $projectEnv -Destination $targetEnv -Force
            Write-Host "  Copied project .env to config\.env"
        } else {
            Write-Warning "  Project .env not found. The app will open setup instructions until config\.env is created."
        }
    } else {
        Write-Host "  Kept existing config\.env"
    }

    if ((Test-Path -LiteralPath $exampleEnv -PathType Leaf) -and -not (Test-Path -LiteralPath $targetExample -PathType Leaf)) {
        Copy-Item -LiteralPath $exampleEnv -Destination $targetExample -Force
    }
}

function Invoke-KgmJPackage {
    param(
        [string]$ProjectRoot
    )

    $jarPath = Join-Path $ProjectRoot "target\$script:KgmJarName"
    if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        throw "Runnable shaded jar was not created: $jarPath"
    }

    $workRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("kgm-ex-jpackage-" + [System.Guid]::NewGuid().ToString("N"))
    $packageTemp = Join-Path $workRoot "output"
    $packageApp = Join-Path $packageTemp $script:KgmAppName
    $inputDir = Join-Path $workRoot "input"
    New-Item -ItemType Directory -Path $packageTemp -Force | Out-Null
    New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $inputDir $script:KgmJarName) -Force

    try {
        Invoke-KgmExternalCommand -FilePath "jpackage" -Arguments @(
            "--type", "app-image",
            "--dest", $packageTemp,
            "--name", $script:KgmAppName,
            "--input", $inputDir,
            "--main-jar", $script:KgmJarName,
            "--main-class", $script:KgmMainClass,
            "--app-version", "1.0.0",
            "--java-options", "-Dfile.encoding=UTF-8"
        ) -Description "jpackage app image"

        if (-not (Test-Path -LiteralPath $packageApp -PathType Container)) {
            throw "jpackage output folder was not created: $packageApp"
        }
        return @{ WorkRoot = $workRoot; PackageApp = $packageApp; JarPath = $jarPath }
    } catch {
        Remove-Item -LiteralPath $workRoot -Recurse -Force -ErrorAction SilentlyContinue
        throw
    }
}

function Install-KgmPackageOutput {
    param(
        [string]$PackageApp,
        [string]$OutputPath
    )

    $sourceExe = Join-Path $PackageApp $script:KgmExeName
    if (-not (Test-Path -LiteralPath $sourceExe -PathType Leaf)) {
        $foundExe = Get-ChildItem -Path $PackageApp -Filter "*.exe" -File | Select-Object -First 1
        if (-not $foundExe) {
            throw "Could not find generated EXE in $PackageApp"
        }
        $sourceExe = $foundExe.FullName
    }

    Copy-Item -LiteralPath $sourceExe -Destination (Join-Path $OutputPath $script:KgmExeName) -Force
    Copy-KgmDirectoryFresh -Source (Join-Path $PackageApp "app") -Destination (Join-Path $OutputPath "app")
    Copy-KgmDirectoryFresh -Source (Join-Path $PackageApp "runtime") -Destination (Join-Path $OutputPath "runtime")
}

function Invoke-KgmExeBuild {
    param(
        [string]$ProjectRoot,
        [string]$OutputDir,
        [switch]$CleanTarget,
        [switch]$PullLatest,
        [string]$Activity
    )

    $script:KgmStepIndex = 0
    if ($PullLatest) {
        $script:KgmTotalSteps = 13
    } else {
        $script:KgmTotalSteps = 12
    }
    $script:KgmActivity = $Activity

    try {
        Set-Location -LiteralPath $ProjectRoot
        if (-not $OutputDir) {
            $OutputDir = "dist\KGM-eX-Employees-App"
        }

        Write-KgmStep -Message "Resolving project and output folders" -Percent 5
        $resolvedOutput = [System.IO.Path]::GetFullPath($OutputDir)

        Write-KgmStep -Message "Checking Java, Maven, and jpackage" -Percent 12
        Check-KgmRequiredTools
        if ($PullLatest) {
            Check-KgmGit
        }

        Write-KgmStep -Message "Checking Java version" -Percent 18
        Require-KgmJava21

        Write-KgmStep -Message "Checking Maven and jpackage versions" -Percent 24
        Check-KgmMaven
        Check-KgmJPackage

        Write-KgmStep -Message "Checking .env and MySQL reachability" -Percent 30
        $envFile = Get-KgmEnvFile -ProjectRoot $ProjectRoot -OutputPath $resolvedOutput
        Check-KgmMySqlReachable -EnvFile $envFile

        if ($PullLatest) {
            Write-KgmStep -Message "Pulling latest code from Git" -Percent 36
            Invoke-KgmExternalCommand -FilePath "git" -Arguments @("pull") -Description "git pull"
        }

        Write-KgmStep -Message "Preparing Maven target folder" -Percent 42
        if ($CleanTarget) {
            Clear-KgmTargetFolder -ProjectRoot $ProjectRoot -OutputPath $resolvedOutput
        } else {
            Write-Host "  Skipping full target cleanup. Use -CleanTarget when needed."
        }

        Write-KgmStep -Message "Building shaded JAR with Maven" -Percent 52
        Invoke-KgmExternalCommand -FilePath "mvn" -Arguments @("package") -Description "Maven package"

        $jarPath = Join-Path $ProjectRoot "target\$script:KgmJarName"
        if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
            throw "Shaded JAR not found: $jarPath"
        }
        Write-Host "  Shaded JAR: $jarPath"

        Write-KgmStep -Message "Creating Windows EXE app image with jpackage" -Percent 68
        $package = Invoke-KgmJPackage -ProjectRoot $ProjectRoot

        Write-KgmStep -Message "Creating protected output folders" -Percent 78
        Ensure-KgmProtectedFolders -ProjectRoot $ProjectRoot -OutputPath $resolvedOutput

        Write-KgmStep -Message "Backing up existing generated app files" -Percent 86
        $backupLabel = "build"
        if ($PullLatest) {
            $backupLabel = "update"
        }
        Backup-KgmAppFiles -OutputPath $resolvedOutput -Label $backupLabel

        Write-KgmStep -Message "Replacing generated EXE, app, and runtime only" -Percent 94
        Install-KgmPackageOutput -PackageApp $package.PackageApp -OutputPath $resolvedOutput

        Write-KgmStep -Message "Preserving config and runtime data folders" -Percent 98
        Copy-KgmEnvFiles -ProjectRoot $ProjectRoot -OutputPath $resolvedOutput
        Remove-Item -LiteralPath $package.WorkRoot -Recurse -Force -ErrorAction SilentlyContinue

        Write-KgmStep -Message "EXE package complete" -Percent 100
        Complete-KgmProgress
        Write-Host ""
        Write-Host "SUCCESS: KGM Ex-Employees app package is ready."
        Write-Host "Output: $resolvedOutput"
        Write-Host "Run: $(Join-Path $resolvedOutput $script:KgmExeName)"
        Write-Host "Shaded JAR: $jarPath"
        Write-Host "Protected folders preserved: config, employees, resources\employees, images\uploads, logs, backups"
    } catch {
        Complete-KgmProgress
        Write-Error "BUILD FAILED: $($_.Exception.Message)"
        exit 1
    }
}
