# Spec tests

Language-neutral test cases that every ontology-graph-spec library must pass. A case is a directory of JSON and YAML
files, and each library has a small runner that maps the cases onto its own API. The Kotlin JVM library is the
reference: its runner regenerates the expected files, which are reviewed and committed like any other change.

```
spec-tests/
  cases/
    documents/<name>/              model.json, model.yaml and alternative forms such as shorthand.yaml
    validate/<set>/<code>/<name>/  input.json or input.yaml, issues.json
  runners/                         go, java and js, each consuming a local build or a published version
```

The Kotlin runner is `src/jvmTest/kotlin/org/neo4j/spec/graph/spectests/SpecTests.kt`.

## Cases

### Valid Model Decoding & Encoding

Each case is formed of a valid longhand model in both JSON and YAML formats (`model.json` and `model.yaml`), and 
optional other JSON/YAML files in the same directoy, representing the same model written differently (e.g. shorthand).

**Tests assert:**
* Decoding any JSON or YAML file then encoding gives `model.json` or `model.yaml`
* Core validation returns no issues
* The reference (Kotlin) runner checks each JSON file against the JSON Schema

### Validation

Each case is formed of an input model and a list of expected validation issues (codes and paths only - not messages).

**Tests assert:**
* The input model is successfully parsed and validation returns the expected list of issues

## Running

```bash
# Kotlin, add -PspecTests.update=true to regenerate the expected files
./gradlew jvmTest --tests 'org.neo4j.spec.graph.spectests.SpecTests'

# Go, local checkout via go.work; GOWORK=off uses the version in go.mod
cd spec-tests/runners/go && go test ./...

# Java, local publication by default; -Pogs.version=<version> uses a published one
./gradlew publishJvmPublicationToMavenLocal publishKotlinMultiplatformPublicationToMavenLocal -Pversion=0.0.0-local
./gradlew -p spec-tests/runners/java test

# JS, local build by default; npm install @neo4j/ontology-graph-spec@<version> --no-save uses a published one
./gradlew jsNodeProductionLibraryDistribution
cd spec-tests/runners/js && npm install && npm test
```

To add a case, create its directory and input, regenerate the expected files with the Kotlin runner, review them, and
run every runner.
