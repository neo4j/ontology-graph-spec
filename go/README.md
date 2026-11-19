# Go GraphSpec

This package contains the Go GraphSpec Library. It consists of:

* **Go-native GraphSpec types** - These are automatically generated from the Kotlin source of truth models
* **Methods for migration and validation** - Enables migration between older model types and latest GraphSpec model, as 
    well as validation of a given GraphSpec model. These methods call into the Kotlin source of truth methods via a
    Kotlin/Native library.

## Usage

This library can be imported into your Go project via the standard `go get`: 

```shell
# Use SSH for git to avoid having to authenticate when fetching this library
git config --global url."git@github.com:".insteadOf "https://github.com/" 

# Fetch Go library
go get github.com/neo4j/ontology-graph-spec/go@vX.Y.Z
```

> [!NOTE]
> Because it is a glibc-based Kotlin/Native library, the runtime must provide `glibc` and `libstdc++`.

All built-in validation sets are available: `SetCore`, `SetLabelled`, `SetConsistentSchema`, `SetMappable`,
`SetBulkImportable`, `SetWellConstrained` and `SetAll`. `SetBulkImportable` checks import token restrictions using the
shared native implementation. When overriding the native library path, use a library built from the matching release.

## How the Kotlin/Native library is loaded

The migration and validation methods call into a Kotlin/Native shared library
(`libgraphdatamodel.so` on Linux, `libgraphdatamodel.dylib` on macOS) at runtime via
[purego](https://github.com/ebitengine/purego). 

By default, the library is automatically embedded in the binary for supported platforms (`linux/amd64`, `linux/arm64`, 
`darwin/arm64`) and loaded on first use, so nothing extra is needed after `go get` on any of those platforms. 

### Using Without Embedding

Automatic embedding can be disabled if needed (e.g. if the runtime is locked down and extracting and loading the 
embedded native library is not possible). Instead of automatic embedding, the relevant shared library can be hosted
in a desired location available to the Go binary at runtime, and pointed to via the `GRAPHDATAMODEL_LIB_PATH` env var.
If this env var is set, the shared library will be loaded from there. The shared libraries are stored in this repo 
under the `go/internal/bridge/lib/` directory.

Optionally, if not using automatic embedding, a Go binary without the bundled library can be built from source with the 
`graphspec_noembed` tag:

```
go build -tags graphspec_noembed ./...
```

> [!NOTE]
> The shared native lib must be provided either via automatic embedding or the `GRAPHDATAMODEL_LIB_PATH` var. If not 
> available, e.g. for an unsupported platform and no `GRAPHDATAMODEL_LIB_PATH`, an error will be returned when run.

### Configuring Memory

Migration and validation run inside the Kotlin/Native library, which has its own heap and garbage collector. That heap
is separate from the Go heap, so is invisible to `runtime.MemStats` and it is not bounded by `GOGC` or `GOMEMLIMIT`. 
It _does_ count against the memory limit of the process or container, so a service that looks healthy in its own heap 
profile can still be killed for exceeding that limit.

The dominant cost is the object graph the library builds while transforming a document, not the document itself. A
model is almost all structure and short strings, so the peak footprint runs at many multiples of the input size (up 
to 80x). Size the memory limit from the largest model expected, rather than from Go heap measurements.

That footprint is per call. Concurrent calls each hold their own, so N migrations in flight need N times the memory,
and the heap ceiling below cannot help with it. A caller migrating large models concurrently should bound that 
concurrency itself.

#### Capping the native heap

The native collector auto-tunes its heap target upwards as the heap grows. By default it has no an upper bound, so 
transient garbage from a very large input model accumulates well past what the model needs. 

The optional `GRAPHSPEC_MAX_HEAP_BYTES` environment variable bounds how far this autotuning goes. It can be helpful
to ensure the native GC proactively clears garbage to avoid significant memory bloat, which becomes more of a problem
with very large inputs.

The value is in decimal bytes, and anything that is not a plain number (`512MB`, `512m`) is ignored without an error.

```shell
GRAPHSPEC_MAX_HEAP_BYTES=536870912   # 512MiB
```

The value is read once, on the first call into the library, and applies for the lifetime of the process. Setting it
from Go with `os.Setenv` works, provided it happens before the first migration or validation call, but setting it in
the container or service definition avoids the ordering question entirely.

> [!NOTE]
> Setting this too low can cause thrashing of the GC, adding significant latency.

## Development

### Building and testing

The Go library can be built and tested as normal:

```shell
go test ./...
```

Tests load the shared library exactly as consumers do. With embedding on (the default) nothing
extra is needed. If building or testing with `graphspec_noembed`, set `GRAPHDATAMODEL_LIB_PATH`
first (see [Using Without Embedding](#using-without-embedding)).

### Generating Kotlin/Native libraries

```shell
./go/scripts/generate-kotlin-native-libs.sh
```

This rebuilds the shared libraries for all supported platforms from the Kotlin source and copies them into 
`internal/bridge/lib/`. This should be done when the underlying Kotlin logic materially changes. 

The regenerated `.so`/`.dylib` files are checked in to git history. They are what `//go:embed` bundles, so a
published tag whose module omits them will fail to build for consumers.

This can also be done via GitHub actions: `gh workflow run update-native-libs.yaml --ref <branch>`

### Generating Go types

```shell
./go/scripts/generate-go-models.sh
```

The GraphModel Go struct and associated structs are automatically generated from the Kotlin source of truth. They live
in the `go/models` package. The pipeline for generating these types is:

_Kotlin_ → _JSON Schema_ → _Go_

The Go structs are generated by the in-repo [schema-generator](../tools/schema-generator/README.md) tool.

> [!NOTE]
> The deployment pipeline automatically checks the Go types are up-to-date. If you receive a pipeline failure relating
> to out-of-date types then rerun the above script.

#### Generating JSON schema

If you want to update the JSON schema separately you can run the individual Gradle command from the repo root:

```shell
./gradlew :generateGraphModelJsonSchema
```

JSON Schema is generated automatically from the Kotlin source of truth via the `kotlinx-schema` library.
