# Neo4j Ontology Graph Specification for .NET

Typed C# models with JSON/YAML decoding, encoding, validation and migration. The models are generated from the
repository's JSON Schema; behavior comes from the same Kotlin core used by the other libraries.

## Requirements

Use .NET 10 with PackageReference. The package bundles native assets for Windows x64 (`win-x64`), Linux x64
(`linux-x64`), Linux ARM64 (`linux-arm64`) and Apple Silicon (`osx-arm64`). Linux requires glibc and libstdc++;
Alpine/musl, Windows ARM64, macOS Intel, browsers, .NET Framework, Native AOT and trimmed applications are not supported.
Normal framework-dependent builds and RID-specific publishes are tested in CI. Single-file deployment is not tested.

Consumers need no JDK, Go installation, separate native download, or Neo4j server. Windows uses only system DLLs
(`KERNEL32.dll` and `msvcrt.dll`). The native core manages its own memory with Kotlin/Native's GC; managed models
hold no native handles and do not need disposal. Independent models can be processed concurrently. Do not mutate a
model while another thread is using it.

## Install and use

The proposed package ID is `Neo4j.OntologyGraphSpec`. Until the first NuGet release, build a local package below and
install it with `dotnet add package Neo4j.OntologyGraphSpec --version 0.0.0-local --source <package-directory>`.
For a published release, use its library version (which is independent of the graph specification version).

```csharp
using Neo4j.OntologyGraphSpec;
using Neo4j.OntologyGraphSpec.Validation;

var model = GraphSpec.Json.DecodeFromString(File.ReadAllText("movies.json"));
foreach (var issue in model.Validate(ValidationSet.Core, ValidationSet.Labelled))
    Console.WriteLine($"{issue.Code} at {issue.Path}: {issue.Message}");
File.WriteAllText("movies.yaml", GraphSpec.Yaml.EncodeToString(model));
```

`GraphSpec.Json` and `GraphSpec.Yaml` have the same API. Decoding expands shorthand and upgrades supported older
versions. Encoding a caller-built model preserves its shorthand when writing graph spec; exporting to another format
expands it in the native copy. Neither encoding nor validation mutates the caller's model.

`model.Validate()` defaults to `Core`. Other sets are `Labelled`, `ConsistentSchema`, `Mappable`, `BulkImportable`,
`WellConstrained` and `All`. Combining sets runs shared rules once. Issues contain `Code`, `Message`, optional `Path`
and `Details`. An empty list means no issues for the selected sets. Validation examines the model, not a database.
Application-specific checks can inspect the generated models in `Neo4j.OntologyGraphSpec.Model`.

```csharp
var migrated = GraphSpec.Json.DecodeFromString(importerJson, ModelType.DataModel);
var exported = GraphSpec.Json.EncodeToString(migrated, ModelType.DataModelWrapped, "3.0.0");
```

Read graph spec 1.0.0/1.1.0 and flat or wrapped Data Importer 2.3.0/2.4.0/3.0.0; write current graph spec or flat/wrapped
Data Importer 3.0.0. Always specify `"3.0.0"` for Data Importer output. Unsupported routes fail. Migration can lose
fields that the destination cannot represent, including arbitrary custom metadata. Open relationship endpoints and
incomplete foreign-key references cannot be exported to Data Importer. YAML supports the same routes as JSON.

Parse, migration, native loading and transport failures throw `GraphSpecException` with an `Operation` and an inner
exception when available. Unsupported platforms throw `PlatformNotSupportedException`. Invalid API arguments throw
the corresponding argument exception. Literal NULs in input strings are rejected at the C boundary; escaped JSON
`\u0000` in property values is supported. Semantic validation problems are returned as issues, not exceptions.

Models use required C# properties, nullable optional values, string enums and typed union interfaces. Use `GraphSpec`
for public documents: direct `System.Text.Json` serialization is a model transport and does not perform normalization,
canonical formatting, or migration. Custom data uses `JsonElement`; clone elements taken from a disposable `JsonDocument`.

## Build and verify

Contributors need JDK 17, the repository Gradle wrapper, Go (see `tools/schema-generator/go.mod`), .NET 10 SDK and
PowerShell 7 (`pwsh` on Linux/macOS). From the repository root:

```powershell
./dotnet/scripts/build-native.ps1 -RuntimeIdentifier win-x64
./dotnet/scripts/generate-models.ps1
Set-Location dotnet
dotnet test --solution Neo4j.OntologyGraphSpec.slnx --configuration Release
dotnet pack src/Neo4j.OntologyGraphSpec/Neo4j.OntologyGraphSpec.csproj -c Release -o artifacts/packages
./scripts/test-package.ps1 -PackagePath artifacts/packages/Neo4j.OntologyGraphSpec.0.0.0-local.nupkg -RuntimeIdentifier win-x64
```

Use the matching RID on other hosts. Linux ARM64 is cross-compiled on an x64 Linux host and executed on ARM64 in CI.
Native compilation is explicit; a normal .NET build does not invoke Gradle. Project references copy the staged host
asset from `dotnet/artifacts/native/<rid>/native/`. NuGet consumers use standard `runtimes/<rid>/native/` resolution.
Local packages contain the assets you staged; release packages require all four RIDs and are tested on every host.

The solution runs both focused adapter tests and the shared corpus in `spec-tests/runners/dotnet`. The latter compares
structural JSON, exact LF YAML and issue details, and tests supported and rejected migrations. Only the Kotlin
reference runner regenerates expected fixtures. Do not hand-edit `Model/Generated/GraphModel.g.cs`; regenerate it
through the Go schema generator and commit the result. CI checks both Go and C# output for drift.

See [the library guide](../docs/modules/libraries/pages/dotnet.adoc) and [release instructions](../docs/RELEASING.md).
