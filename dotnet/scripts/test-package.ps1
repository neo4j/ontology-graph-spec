param(
    [Parameter(Mandatory = $true)][string]$PackagePath,
    [Parameter(Mandatory = $true)]
    [ValidateSet('win-x64', 'linux-x64', 'linux-arm64', 'osx-arm64')][string]$RuntimeIdentifier,
    [switch]$RequireAllRuntimes
)

$ErrorActionPreference = 'Stop'
$package = (Resolve-Path -LiteralPath $PackagePath).Path
$archive = [IO.Compression.ZipFile]::OpenRead($package)
try {
    $entries = @($archive.Entries.FullName)
    $required = @('lib/net10.0/Neo4j.OntologyGraphSpec.dll', 'README.md')
    $assets = @{
        'win-x64' = 'graphdatamodel.dll'
        'linux-x64' = 'libgraphdatamodel.so'
        'linux-arm64' = 'libgraphdatamodel.so'
        'osx-arm64' = 'libgraphdatamodel.dylib'
    }
    $rids = if ($RequireAllRuntimes) { @($assets.Keys) } else { @($RuntimeIdentifier) }
    foreach ($rid in $rids) { $required += "runtimes/$rid/native/$($assets[$rid])" }
    foreach ($entry in $required) {
        if ($entry -notin $entries) { throw "Package is missing $entry" }
    }
    $reader = [IO.StreamReader]::new($archive.GetEntry('Neo4j.OntologyGraphSpec.nuspec').Open())
    try { [xml]$manifest = $reader.ReadToEnd() } finally { $reader.Dispose() }
    $version = $manifest.package.metadata.version
} finally { $archive.Dispose() }

# An isolated application and cache ensure a project reference, stale restore or source-tree DLL cannot mask a bad package.
$scratch = Join-Path ([IO.Path]::GetTempPath()) ('graphspec-smoke-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $scratch | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot '../tests/PackageConsumer/PackageConsumer.csproj') -Destination $scratch
Copy-Item -LiteralPath (Join-Path $PSScriptRoot '../tests/PackageConsumer/Program.cs') -Destination $scratch
$feed = Join-Path $scratch 'feed'
New-Item -ItemType Directory -Path $feed | Out-Null
Copy-Item -LiteralPath $package -Destination $feed
@'
<configuration><packageSources><clear /><add key="local" value="feed" /><add key="nuget.org" value="https://api.nuget.org/v3/index.json" /></packageSources><packageSourceMapping><packageSource key="local"><package pattern="Neo4j.OntologyGraphSpec" /></packageSource><packageSource key="nuget.org"><package pattern="Microsoft.*" /></packageSource></packageSourceMapping></configuration>
'@ | Set-Content -LiteralPath (Join-Path $scratch 'NuGet.Config')

Push-Location $scratch
try {
    dotnet build -c Release "-p:PackageVersion=$version" "-p:RestorePackagesPath=$scratch/packages"
    if ($LASTEXITCODE -ne 0) { throw 'Package consumer build failed.' }
    dotnet bin/Release/net10.0/PackageConsumer.dll $RuntimeIdentifier
    if ($LASTEXITCODE -ne 0) { throw 'Package consumer execution failed.' }
    dotnet publish -c Release -r $RuntimeIdentifier --self-contained false "-p:PackageVersion=$version" "-p:RestorePackagesPath=$scratch/packages" -o published
    if ($LASTEXITCODE -ne 0) { throw 'RID-specific package publish failed.' }
    dotnet published/PackageConsumer.dll $RuntimeIdentifier
    if ($LASTEXITCODE -ne 0) { throw 'Published package consumer failed.' }
    Write-Output "Verified $package ($RuntimeIdentifier). Consumer retained at $scratch"
} finally { Pop-Location }
