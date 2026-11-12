# Spec tests

Language-neutral test cases that every ontology-graph-spec library must pass. A case is a directory of JSON and YAML
files, and each library has a small runner that maps the cases onto its own API. The Kotlin JVM library is the
reference: its runner regenerates the expected files, which are reviewed and committed like any other change.

```
spec-tests/
  cases/
    documents/<name>/              model.json, model.yaml and alternative forms such as shorthand.yaml
    validate/<set>/<name>/         input.json or input.yaml, issues.json
  runners/
    kotlin/                        the reference, built against this project, regenerates expected files
    go/ java/ js/                  each consuming a local build or a published version
```

## Cases

### Valid Model Decoding & Encoding

Each case is formed of a valid longhand model in both JSON and YAML formats (`model.json` and `model.yaml`), and 
optional other JSON/YAML files in the same directoy, representing the same model written differently (e.g. shorthand).

**Tests assert:**
* Decoding any JSON or YAML file then encoding gives `model.json` or `model.yaml`
* Core validation returns no issues
* The reference (Kotlin) runner checks each JSON file against the JSON Schema

> [!NOTE]
> The `sample-*` cases are the sample models shown in the UI. Each `shorthand.json` is a verbatim copy from the 
> upx project. 

### Validation

Each case is formed of an input model and a list of expected validation issues (codes and paths only - not messages).

Cases group related issues, such as `core/constraints` or `core/references`, and each issue code is reported by at
least one case under the set that reports it. Inputs are JSON, as the Go runner can't read YAML. Some validations only
run when another found nothing, so a case shouldn't mix issues from validations that depend on each other.

**Tests assert:**
* The input model is successfully parsed and validation returns the expected list of issues

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

To add a case, create its directory and input, regenerate the expected files with the Kotlin runner, review them, and
run every runner.
