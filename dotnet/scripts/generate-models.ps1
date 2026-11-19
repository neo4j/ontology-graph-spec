param([switch]$SkipSchema)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Push-Location $repoRoot
try {
    if (-not $SkipSchema) {
        $gradle = if ($env:OS -eq 'Windows_NT') { './gradlew.bat' } else { './gradlew' }
        & $gradle :generateGraphModelJsonSchema --no-daemon --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Schema generation failed.' }
    }
    & go -C tools/schema-generator run . --language csharp --schema ../../ontology-graph-spec.schema.json --out ../../dotnet/src/Neo4j.OntologyGraphSpec/Model/Generated/GraphModel.g.cs --namespace Neo4j.OntologyGraphSpec.Model
    if ($LASTEXITCODE -ne 0) { throw 'C# model generation failed.' }
} finally { Pop-Location }
