# ontology-graph-spec (Rust)

Read, validate and write [Neo4j Ontology Graph Specification](../README.md) documents: ontologies
for Neo4j written as YAML or JSON.

This is a native Rust implementation of the format, not a binding to the Kotlin reference. It
needs no native library at runtime. It passes the [spec tests](../spec-tests/README.md), the test
cases every ontology-graph-spec library runs, with byte-identical output.

## Install

```shell
cargo add ontology-graph-spec
```

Requires Rust 1.85 or later. Tested on Linux (x86_64), macOS (arm64) and Windows (x86_64), and
builds for `wasm32-unknown-unknown`, including with the `schema-validation` feature. Until the
first crates.io release, depend on the repository:

```toml
[dependencies]
ontology-graph-spec = { git = "https://github.com/neo4j/ontology-graph-spec.git" }
```

## Read a document

Decode JSON or YAML into the same typed model. The format's shorthands (a node's `label`, a
property's `key`, `unique`, `must_exist`) are expanded on decode, so every way of writing the
same model decodes to the same model.

```rust
use ontology_graph_spec::GraphSpec;

let json = r#"{
    "schema": "https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json",
    "version": "1",
    "id": "movies",
    "nodes": {
        "Movie": {
            "label": "Movie",
            "properties": { "title": { "type": "STRING", "must_exist": true } }
        }
    }
}"#;

let model = GraphSpec::decode(json)?;
```

The same model as YAML:

```rust
let yaml = "\
schema: https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json
version: \"1\"
id: movies
nodes:
  Movie:
    label: Movie
    properties:
      title:
        type: STRING
        must_exist: true
";

let model = GraphSpec::decode(yaml)?;
```

Both decode to a model whose `Movie` node carries `labels.identifier = "Movie"` and an existence
constraint on `title`. Documents written against older released schema versions are read at the
latest one.

A document that parses as strict JSON is JSON; anything else is YAML. `GraphSpec::decode`
detects the syntax itself, so user-supplied files of either syntax work:

```rust
let model = GraphSpec::decode(content)?;
```

`detect_document_format` returns the codec, for when you need to know which syntax it was, for
example to encode back in the same one:

```rust
let model = GraphSpec::decode(content)?;
let encoded = detect_document_format(content).encode(&model)?;
```

## Work with the model

The model is plain typed data: public fields, maps in document order.

```rust
for (node_id, node) in &model.nodes {
    let label = node.labels.identifier.as_deref().unwrap_or("<unnamed>");
    println!("{node_id} ({label}): {} properties", node.properties.len());
}
```

## Validate

Validation returns a list of issues. The list is empty when the model has no problems. Each issue
carries a stable `code`, a `path` into the model as a JSON pointer, a human-readable `message`
and `details` about the value at fault. `Issue` implements `Display` and `std::error::Error`, so
it drops straight into error reporting.

```rust
use ontology_graph_spec::validate::ValidationSet;

let issues = model.validate(&ValidationSet::core());
for issue in &issues {
    println!("{issue}");  // e.g. "duplicate_node_alias at /nodes/Person/aliases/1: ..."
}
```

`core` holds the rules every model must follow. The other built-in sets add stricter or more
opinionated ones:

| Set | Checks |
|---|---|
| `core` | Released schema url, references resolve, constraint and index details consistent, relationship counts sensible, text within length limits, no repeated aliases, labels or key properties |
| `labelled` | Every node has an identifier label, every relationship a type and from and to nodes |
| `consistent_schema` | Property names set and unique, indexes and constraints have properties and unique names, no duplicated or conflicting constraints |
| `mappable` | Node mappings have keys, mapping ids unique, dependencies resolve without cycles, table columns named and typed |
| `bulk_importable` | Neo4j bulk import restrictions on label and type tokens, mapping keys and composite constraint properties |
| `well_constrained` | Opinionated: every node has properties and a constraint, no `UNIQUE`/`EXISTS` redundant next to a `KEY` |

Combine them, check everything, or add your own validator:

```rust
let set = ValidationSet::core().union(ValidationSet::labelled());
let set = ValidationSet::all();
let set = ValidationSet::core().with_validator(my_validator);
```

## Write a document

Encoding always writes the longhand form. JSON is pretty-printed with four-space indents. YAML is
written exactly as the reference implementation writes it.

```rust
use ontology_graph_spec::{encode_json_compact, model_to_json_value};

let json = GraphSpec::Json.encode(&model)?;   // pretty
let json = encode_json_compact(&model)?;                // compact
let value = model_to_json_value(&model)?;               // the document as a JSON value
let yaml = GraphSpec::Yaml.encode(&model)?;
```

`yaml_to_json_value` gives the JSON value a YAML document holds, for converting between the two
syntaxes or for validating a document before decoding it.

## Validating against the JSON Schema

The validator sets are the semantic checks. The `schema-validation` feature adds the structural
gate: the published JSON Schema of the format, covering closed objects and enums, required
fields, types. Together they are the whole contract.

```toml
[dependencies]
ontology-graph-spec = { version = "x.y.z", features = ["schema-validation"] }
```

```rust
use ontology_graph_spec::validate_document_schema;

let value = yaml_to_json_value(content)?;   // or serde_json::from_str for JSON
validate_document_schema(&value)?;          // a SchemaViolations error, one line per violation
let model = GraphSpec::decode(content)?;
```

The error implements `Display` (the whole list, indented) and `std::error::Error`, so it
propagates with `?`. Its `violations` field is the list, for reporting each violation in its own
place.

## What it covers

Decoding, encoding and validation of models at the latest schema version, including documents
from older released versions, which the format defines to read the same at the latest one.

Not covered, yet:

- **Migrations** to and from the Aura Data Importer data model formats, and encoding back to an
  older graph spec version. Use the JVM, JS/TS or Go libraries for those.
- The reference's **temporary migration validation sets**, which are deliberately not carried into
  the other language libraries.

## Development

```shell
cd rust && cargo test                        # model, codecs, validators
cd ../spec-tests/runners/rust && cargo test  # the spec tests every library runs
```

`cargo run --example diff -- <case-file>` prints a case document decoded and re-encoded in both
formats, for debugging differences.
