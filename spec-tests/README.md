# Spec tests

Language-neutral test cases that every ontology-graph-spec library must pass. A case is a directory of JSON and YAML
files, and each library has a small runner that maps the cases onto its own API. The Kotlin JVM library is the
reference: its runner regenerates the expected files, which are reviewed and committed like any other change.

```
spec-tests/
  cases/
    documents/<name>/              model.json, model.yaml and alternative forms such as shorthand.yaml
    validate/<set>/<name>/         input.json, input.yaml and expected issues.json
    migrate/<name>/                case.json, input.json and expected.json on success
  runners/
    kotlin/                        the reference, built against this project, regenerates expected files
    go/ java/ js/ dotnet/                  each consuming a local build or a published version
```

## Cases

### Valid Model Decoding & Encoding

Each case is formed of a valid longhand model in both JSON and YAML formats (`model.json` and `model.yaml`), and 
optional other JSON/YAML files in the same directory, representing the same model written differently (e.g. shorthand).

**Tests assert:**
* Decoding any JSON or YAML file then encoding gives `model.json` or `model.yaml`
* Core validation returns no issues
* The reference (Kotlin) runner checks each JSON file against the JSON Schema

> [!NOTE]
> The `sample-*` cases are the sample models shown in the UI. Each `shorthand.json` is a verbatim copy from the 
> upx project. 

### Validation

Each case is formed of an input model and a list of expected validation issues (codes, paths and details; not exact message wording). All seven built-in sets have cases.

**Tests assert:**
* The input model is successfully parsed and validation returns the expected list of issues

Issue comparison is order-independent but preserves duplicate occurrences. Absent and empty `details` are equivalent;
nonempty details must match. Document JSON is compared structurally, preserving array order and value types. YAML is
compared exactly and committed with LF line endings so Windows checks the same bytes.

### Migration

`migrate/<name>/case.json` contains `source_type`, `target_type`, `target_version` and boolean `fails`. The runner decodes
`input.json` using the source type and encodes to the target type/version. Successful cases compare structurally with
Kotlin-generated `expected.json`; failure cases require an error with a nonblank diagnostic, not a fixed stack trace.
These fixtures are JSON-only so Go can run them too. .NET and Kotlin also test YAML migration at the adapter level.

Cases cover graph spec 1.0.0 upgrades, flat and wrapped Data Importer 2.3.0/2.4.0/3.0.0 imports, flat and wrapped 3.0.0
exports, custom-data loss, unsupported reverse routes and unrepresentable graph-spec input. Export expectations are
not assumed to be lossless. Only the Kotlin reference runner updates expected files.

## Running

```bash
# Kotlin, add -PspecTests.update=true to regenerate the expected files
./gradlew :spec-tests-kotlin:test

# Go, local checkout via go.work; GOWORK=off uses the version in go.mod
cd spec-tests/runners/go && go test ./...

# Java, local publication by default; -Pogs.version=<version> uses a published one
./gradlew publishJvmPublicationToMavenLocal publishKotlinMultiplatformPublicationToMavenLocal -Pversion=0.0.0-local
./gradlew -p spec-tests/runners/java test

# JS, local build by default; npm install @neo4j/ontology-graph-spec@<version> --no-save uses a published one
./gradlew jsNodeProductionLibraryDistribution
cd spec-tests/runners/js && npm install && npm test
```

For .NET, stage the native asset and run both test projects from `dotnet/` (PowerShell 7):

```powershell
./dotnet/scripts/build-native.ps1 -RuntimeIdentifier win-x64
Set-Location dotnet
dotnet test --solution Neo4j.OntologyGraphSpec.slnx --configuration Release
```

Use the host RID from [the .NET README](../dotnet/README.md). The runner copies the shared corpus into test output,
never maintains a second fixture set, and fails if fixtures are missing. The Go runner is JSON-only and must use
native libraries rebuilt from this commit for newly exposed validation sets.

To add a case, create its directory and input, regenerate the expected files with the Kotlin runner, review them, and
run every runner.
