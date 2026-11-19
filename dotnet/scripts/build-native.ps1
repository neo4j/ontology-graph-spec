param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('win-x64', 'linux-x64', 'linux-arm64', 'osx-arm64')]
    [string]$RuntimeIdentifier,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$targets = @{
    'win-x64' = @('MingwX64', 'mingwX64', 'graphdatamodel.dll')
    'linux-x64' = @('LinuxX64', 'linuxX64', 'libgraphdatamodel.so')
    'linux-arm64' = @('LinuxArm64', 'linuxArm64', 'libgraphdatamodel.so')
    'osx-arm64' = @('MacosArm64', 'macosArm64', 'libgraphdatamodel.dylib')
}
$target = $targets[$RuntimeIdentifier]
Push-Location $repoRoot
try {
    if (-not $SkipBuild) {
        $gradle = if ($env:OS -eq 'Windows_NT') { './gradlew.bat' } else { './gradlew' }
        & $gradle "linkReleaseShared$($target[0])" --no-daemon --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Native build failed.' }
    }
    $source = Join-Path $repoRoot "build/bin/$($target[1])/releaseShared/$($target[2])"
    if (-not (Test-Path -LiteralPath $source -PathType Leaf)) { throw "Missing native build: $source" }
    $destination = Join-Path $repoRoot "dotnet/artifacts/native/$RuntimeIdentifier/native"
    New-Item -ItemType Directory -Force -Path $destination | Out-Null
    Copy-Item -LiteralPath $source -Destination (Join-Path $destination $target[2]) -Force
    Write-Output "Staged $RuntimeIdentifier from the current build."
} finally { Pop-Location }
