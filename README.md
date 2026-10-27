# Neo4j Ontology Graph Specification

A format and library for defining, validating and migrating ontologies for Neo4j, expressed as YAML or JSON, with
support for Java, Kotlin, JavaScript, TypeScript & Go.

## What is an ontology?

An **ontology** is a formal description of the things in a domain and how they relate: the kinds of entities that
exist, the relationships between them, and the properties and rules that apply to each.

For Neo4j, that is the **graph model** behind a graph, written down explicitly:

| Ontology concept | In Neo4j |
|---|---|
| Kind of entity | Node (identified by one or more labels) |
| Kind of connection | Relationship type, with its start and end nodes |
| Attribute | Property, with a type |
| Rule | Constraints (uniqueness, existence, keys) and indexes |

For example, "an `Actor` has an `id` and a `name`, and can have an `ACTED_IN` relationship to a `Movie`", forms a small ontology.

Neo4j is schema-optional, so a graph's model often lives only in people's heads, import scripts or diagrams. A
machine-readable ontology gives tools and people one shared source of truth for designing models, importing data into
the right shape, validating consistency, and exchanging models between tools.

## What's in this repository?

- **A format**: ontologies as YAML or JSON, described by a published [JSON Schema](ontology-graph-spec.schema.json).
- **Libraries**: one Kotlin codebase published for JVM, JS/TS and Go, providing:
  - **Encoding and decoding** between YAML/JSON and a typed `GraphModel`
  - **Validation**, reporting issues in a model
  - **Migration** from older or related model formats to the latest version, and back

### Example

An excerpt of a model describing actors, movies and the relationship between them (JSON):

```json
{
  "schema": "https://neo4j.com/docs/ontology-graph-spec/1.0.0/schema.json",
  "version": "1",
  "id": "movies",
  "name": "Movies",
  "description": "People and the films they appear in.",
  "nodes": {
    "Actor": {
      "label": "Actor",
      "description": "A person who performs in films.",
      "aliases": ["Performer"],
      "reference": "https://schema.org/Person",
      "properties": {
        "id":   { "type": "STRING", "key": true },
        "name": { "type": "STRING", "must_exist": true, "description": "Full name." }
      },
      "extensions": { "custom": { "owner": "catalogue-team" } }
    },
    "Movie": {
      "label": "Movie",
      "reference": "https://schema.org/Movie",
      "properties": {
        "id":    { "type": "STRING", "key": true },
        "genre": { "type": "STRING", "one_of": ["DRAMA", "COMEDY", "THRILLER"] }
      }
    }
  },
  "relationships": {
    "ACTED_IN": {
      "type": "ACTED_IN",
      "description": "An actor appeared in a film.",
      "reference": "https://schema.org/actor",
      "from": { "node": "Actor", "label": "Actor", "min_count": 0 },
      "to":   { "node": "Movie", "label": "Movie", "min_count": 1 },
      "properties": { "roles": { "type": "STRING" } }
    }
  }
}
```

> [!NOTE]
> The example uses shorthand: a node's `label` and a property's `key`, `unique` and `must_exist`. These are accepted
> when decoding and expanded into `labels.identifier` and constraints.

> [!NOTE]
> Cardinality (`min_count`, `max_count`, `count`) and `one_of` are part of the model but not yet enforced in the database.

See [go/model/testdata/graph-spec-example.json](go/model/testdata/graph-spec-example.json) for a complete model,
including extensions.

## Installing

### Gradle

```gradle
implementation("org.neo4j.spec.graph:ontology-graph-spec:x.y.z")
```

### Maven

```xml
<dependency>
    <groupId>org.neo4j.spec.graph</groupId>
    <artifactId>ontology-graph-spec</artifactId>
    <version>x.y.z</version>
</dependency>
```

### npm

```bash
npm install -D @neo4j/ontology-graph-spec
```

### Go

```bash
go get github.com/neo4j/ontology-graph-spec/go@vX.Y.Z
```

The Go library bundles the Kotlin/Native library, which is embedded and loaded automatically. The runtime needs
`glibc` and `libstdc++`. See the [Go README](go/README.md) for details, including running without embedding.

## Usage

Every library can **decode** a document into a `GraphModel` and **validate** it. The JVM and JS/TS libraries can also
**encode** a model back to a document, using `GraphSpec.Json` or `GraphSpec.Yaml`. Go works with JSON.

### Kotlin

```kotlin
import org.neo4j.spec.graph.GraphSpec
import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.validate.Validations

// Decode (use GraphSpec.Yaml for YAML)
val model: GraphModel = GraphSpec.Json.decodeFromString(json)

// Validate
val issues = model.validate(Validations.core)
issues.forEach { println("${it.code}: ${it.message} (${it.path})") }

// Encode
val encoded: String = GraphSpec.Yaml.encodeToString(model)
```

### Java

```java
import org.neo4j.spec.graph.GraphSpec;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.Type;
import org.neo4j.spec.graph.model.Version;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validations;

// Decode (use GraphSpec.Yaml.INSTANCE for YAML)
GraphModel model = GraphSpec.Json.INSTANCE.decodeFromString(json, Type.GRAPH_SPEC);

// Validate
for (Issue issue : model.validate(Validations.Companion.getCore())) {
    System.out.println(issue.getCode() + ": " + issue.getMessage());
}

// Encode
String encoded = GraphSpec.Yaml.INSTANCE.encodeToString(model, Type.GRAPH_SPEC, Version.LATEST);
```

### JavaScript / TypeScript

```typescript
import { GraphSpec, GraphModel, Validations } from "@neo4j/ontology-graph-spec";

// Decode (use GraphSpec.Yaml for YAML)
const model: GraphModel = GraphSpec.Json.decodeFromString(json);

// Validate
const issues = GraphModel.validate(model, Validations.core);
issues.forEach((issue) => console.log(`${issue.code}: ${issue.message}`));

// Encode
const encoded: string = GraphSpec.Yaml.encodeToString(model);
```

### Go

```go
import (
	"encoding/json"

	"github.com/neo4j/ontology-graph-spec/go/model"
	"github.com/neo4j/ontology-graph-spec/go/validation"
)

// Decode
var graph model.GraphModel
if err := json.Unmarshal(raw, &graph); err != nil {
	return err
}

// Validate
issues, err := validation.Validate(graph, validation.SetCore)
if err != nil {
	return err
}
for _, issue := range issues {
	fmt.Printf("%s: %s (%s)\n", issue.Code, issue.Message, issue.Path)
}
```

## Documentation

The docs live in `docs/` as an Antora site. To build and preview them locally:

```bash
cd docs
npm install
npm start
```

This builds the site and serves it at http://localhost:8000, rebuilding and restarting the server on changes.
Use `npm run build:preview` to build without serving, or `npm run serve` to serve an existing build.

## Interoperability

The format covers the parts of RDFS and SHACL that describe a domain, not all of either. See
[docs/modules/format/pages/interoperability.adoc](docs/modules/format/pages/interoperability.adoc) for the
construct-by-construct tables and what each verdict means.

Construct counts by verdict:

| Verdict | RDFS | SHACL |
|---|---|---|
| covered | 6 | 5 |
| partially covered | 1 | 1 |
| lossy | 0 | 4 |
| carried | 0 | 8 |
| different mechanism | 0 | 1 |
| excluded | 2 | 3 |
| n/a | 1 | 0 |
| total | 10 | 22 |

## Repository layout

| Path | Contents |
|---|---|
| `src/commonMain` | Core Kotlin model, codecs, validation and migrations |
| `src/jvmMain`, `src/jsMain` | Platform-specific code for JVM and JS/TS |
| `src/bridge` | Kotlin/Native bridge used by the Go library |
| `go/` | Go library and generated Go types |
| `ontology-graph-spec.schema.json` | JSON Schema for the format |
| `docs/` | Documentation |

## Contributing

Releases are driven by labelling PRs; see [docs/RELEASING.md](docs/RELEASING.md).

## License

Apache License 2.0. See [LICENSE](LICENSE).
