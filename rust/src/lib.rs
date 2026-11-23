/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//! A format and library for defining, validating and exchanging ontologies for Neo4j, expressed
//! as YAML or JSON.
//!
//! An **ontology** is a formal description of the things in a domain and how they relate. For
//! Neo4j, that is the graph model behind a graph: node labels, relationship types, properties and
//! the rules and indexes that apply to each.
//!
//! This library **decodes** a YAML or JSON document into a typed [`GraphModel`], expanding the
//! format's shorthands so every form of the same model decodes the same way, **validates** the
//! model against sets of rules, and **encodes** the model back to a document in the canonical
//! longhand form.
//!
//! ```no_run
//! # fn main() -> Result<(), Box<dyn std::error::Error>> {
//! use ontology_graph_spec::GraphSpec;
//! use ontology_graph_spec::validate::ValidationSet;
//!
//! // Decode (GraphSpec::Yaml for YAML)
//! let model = GraphSpec::decode(r#"{"schema":"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json","version":"1","id":"movies"}"#)?;
//!
//! // Validate
//! for issue in model.validate(&ValidationSet::core()) {
//!     println!("{}: {} ({})", issue.code, issue.message, issue.path.unwrap_or_default());
//! }
//!
//! // Encode
//! let encoded = GraphSpec::Yaml.encode(&model)?;
//! # let _ = encoded;
//! # Ok(())
//! # }
//! ```

pub mod codec;
pub mod model;
pub mod validate;

pub use codec::{
    decode_json, decode_yaml, detect_document_format, encode_json_compact, model_to_json_value,
    yaml_to_json_value, GraphSpec,
};
#[cfg(feature = "schema-validation")]
pub use codec::{validate_document_schema, SchemaViolations};
pub use model::GraphModel;
pub use validate::{Issue, Validation, ValidationSet};
