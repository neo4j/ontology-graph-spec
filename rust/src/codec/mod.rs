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

//! Decoding and encoding between documents and a typed [`GraphModel`].
//!
//! Decoding accepts both the longhand form and its shorthands (a node's `label`, a property's
//! `key`, `unique` and `must_exist`), which are expanded so every form of the same model decodes
//! to the same model. Encoding writes the model as it stands: a decoded model is longhand, and
//! shorthand fields set by hand are written as-is, as the reference implementation does.
//!
//! ```no_run
//! # fn main() -> Result<(), Box<dyn std::error::Error>> {
//! use ontology_graph_spec::GraphSpec;
//! use ontology_graph_spec::validate::ValidationSet;
//!
//! let model = GraphSpec::decode(r#"{"schema":"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json","version":"1","id":"movies"}"#)?;
//! let issues = model.validate(&ValidationSet::core());
//! println!("{} issues", issues.len());
//! let encoded = GraphSpec::Yaml.encode(&model)?;
//! # Ok(())
//! # }
//! ```

pub(crate) mod json_writer;
pub(crate) mod schema;
pub(crate) mod yaml_quotes;
pub(crate) mod yaml_writer;

use crate::model::version::{self, GRAPH_SPEC_VERSIONS, LATEST};
use crate::model::GraphModel;

/// Which format to decode from or encode to.
#[derive(Copy, Clone, Debug, PartialEq, Eq, Hash)]
pub enum GraphSpec {
    /// JSON.
    Json,
    /// YAML.
    Yaml,
}

/// Detects which syntax a document is written in: a strict JSON parse decides it, anything that
/// doesn't parse as JSON is YAML.
#[must_use]
pub fn detect_document_format(content: &str) -> GraphSpec {
    match serde_json::from_str::<serde_json::Value>(content) {
        Ok(_) => GraphSpec::Json,
        Err(_) => GraphSpec::Yaml,
    }
}

impl GraphSpec {
    /// Decodes a document of either syntax into a model, expanding any shorthand. A strict JSON
    /// parse decides the syntax: a document that parses as JSON is JSON, anything else is YAML.
    /// To pin a syntax instead of detecting it, use [`decode_json`] or [`decode_yaml`].
    ///
    /// # Errors
    ///
    /// When the document isn't valid JSON or YAML, doesn't fit the model, or uses an unreleased
    /// schema url.
    pub fn decode(content: &str) -> Result<GraphModel, Error> {
        match detect_document_format(content) {
            GraphSpec::Json => decode_json(content),
            GraphSpec::Yaml => decode_yaml(content),
        }
    }

    /// Encodes a model as a document in this format. The model is written as it stands: a
    /// decoded model is longhand, shorthand fields set by hand are written as-is.
    ///
    /// # Errors
    ///
    /// When the model can't be encoded, e.g. its schema url isn't a released one.
    pub fn encode(&self, model: &GraphModel) -> Result<String, Error> {
        match self {
            GraphSpec::Json => encode_json(model),
            GraphSpec::Yaml => encode_yaml(model),
        }
    }
}

/// Decodes a JSON document into a model, expanding any shorthand. For documents of either
/// syntax, [`GraphSpec::decode`] detects the syntax itself.
///
/// # Errors
///
/// See [`GraphSpec::decode`].
pub fn decode_json(content: &str) -> Result<GraphModel, Error> {
    let mut model: GraphModel = serde_json::from_str(content).map_err(Error::Json)?;
    expand(&mut model)?;
    Ok(model)
}

/// Decodes a YAML document into a model, expanding any shorthand. Plain scalars a standard
/// YAML parser would resolve to null or a boolean but the reference implementation reads as
/// strings, such as `Null` or `TRUE`, are read the same way here. For documents of either
/// syntax, [`GraphSpec::decode`] detects the syntax itself.
///
/// # Errors
///
/// See [`GraphSpec::decode`].
pub fn decode_yaml(content: &str) -> Result<GraphModel, Error> {
    let mut model: GraphModel = from_yaml_str(content)?;
    expand(&mut model)?;
    Ok(model)
}

/// Encodes a model as a JSON document, pretty-printed with four-space indents. The model is
/// written as it stands, see [`GraphSpec::encode`].
///
/// # Errors
///
/// See [`GraphSpec::encode`].
pub fn encode_json(model: &GraphModel) -> Result<String, Error> {
    let value = model_to_json_value(model)?;
    Ok(json_writer::to_pretty_string(&value))
}

/// Encodes a model as a compact JSON document, keys in document order.
///
/// The same model as [`encode_json`] writes, without the pretty-printing.
///
/// # Errors
///
/// See [`GraphSpec::encode`].
pub fn encode_json_compact(model: &GraphModel) -> Result<String, Error> {
    let value = model_to_json_value(model)?;
    serde_json::to_string(&value).map_err(Error::Json)
}

/// Decodes a YAML document into the JSON value it holds, without fitting the model.
///
/// The same plain-scalar reading as [`decode_yaml`] applies, so `Null` stays the string "Null".
/// For validating a document against the JSON Schema before decoding it into the model, or for
/// converting between the two syntaxes without a model.
///
/// # Errors
///
/// When the document isn't valid YAML.
pub fn yaml_to_json_value(content: &str) -> Result<serde_json::Value, Error> {
    from_yaml_str(content)
}

/// Reads a YAML document with the same plain-scalar reading every YAML entry point uses: the
/// re-quote pass first, so `Null` stays the string "Null" everywhere.
fn from_yaml_str<T: serde::de::DeserializeOwned>(content: &str) -> Result<T, Error> {
    let amended = yaml_quotes::requote_plain_scalars(content);
    serde_norway::from_str(&amended).map_err(Error::Yaml)
}

/// The ways a document violates the published JSON Schema.
#[cfg(feature = "schema-validation")]
pub use schema::SchemaViolations;

/// Checks a document against the published JSON Schema of the format.
///
/// The schema [`include_str`]s the repository's own `ontology-graph-spec.schema.json`, so it is
/// always the one this library reads. This is the structural gate: closed objects and enums,
/// required fields, types. The [`crate::validate`] sets are the semantic checks beyond it.
/// Both are needed for the whole contract, as the spec tests assert.
///
/// Available with the `schema-validation` feature.
///
/// # Errors
///
/// A [SchemaViolations] error, one line per violation, each naming where in the document.
#[cfg(feature = "schema-validation")]
pub fn validate_document_schema(value: &serde_json::Value) -> Result<(), SchemaViolations> {
    schema::validate_document(value)
}
/// Encodes a model as a YAML document, written exactly as the reference implementation writes
/// it. The model is written as it stands, see [`GraphSpec::encode`].
///
/// # Errors
///
/// See [`GraphSpec::encode`].
pub fn encode_yaml(model: &GraphModel) -> Result<String, Error> {
    let value = model_to_json_value(model)?;
    Ok(yaml_writer::write(
        &value,
        &yaml_writer::YamlPrintOptions::model_format(),
    ))
}

/// Brings a decoded document to the model the library works with: documents from older released
/// schema versions are re-stamped to the latest one (the reference's graph-spec migrations are
/// no-ops that only bump the version), and any shorthand is expanded into its longhand form.
/// An error means no migration path covers the document's version.
fn expand(model: &mut GraphModel) -> Result<(), Error> {
    if let Some(latest) = migrated_schema_url(&model.schema)? {
        model.schema = latest;
    }
    model.longhand();
    Ok(())
}

/// The latest schema url when [schema] names an older released graph spec version: the
/// graph-spec migrations in the reference implementation are no-ops that only re-stamp the
/// schema url, so the format of an older released version reads the same at the latest one.
/// `None` when the model is already at the latest version; an error when the url names an
/// unreleased or unrelated version no migration path covers.
fn migrated_schema_url(schema: &str) -> Result<Option<String>, Error> {
    let version = version::parse_schema_version(schema).map_err(Error::Unsupported)?;
    let version = normalize_semver(&version);
    if version == LATEST {
        return Ok(None);
    }
    if GRAPH_SPEC_VERSIONS.contains(&version.as_str()) {
        Ok(Some(version::latest_schema_url()))
    } else {
        Err(Error::Unsupported(format!(
            "the schema url names version {version}, which this library can't migrate to the \
             latest version ({LATEST})"
        )))
    }
}

/// The version without its prerelease or build suffix, as migrations compare them: `1.1.0-rc.1`
/// is `1.1.0`.
fn normalize_semver(version: &str) -> String {
    let semver = version.split(['-', '+']).next().unwrap_or("");
    if semver.matches('.').count() > 1 {
        if let Some((head, _)) = semver.rsplit_once('.') {
            return format!("{head}.0");
        }
    }
    semver.to_owned()
}

/// Encodes a model as the JSON value of its document, keys in document order: the value
/// [encode_json](fn@encode_json) and [`encode_json_compact`] write. The model is written as it
/// stands, see [`GraphSpec::encode`].
///
/// # Errors
///
/// See [`GraphSpec::encode`].
pub fn model_to_json_value(model: &GraphModel) -> Result<serde_json::Value, Error> {
    let mut value = serde_json::to_value(model).map_err(Error::Json)?;
    if let Some(latest) = migrated_schema_url(&model.schema)? {
        value["schema"] = serde_json::Value::String(latest);
    }
    Ok(value)
}

/// What went wrong decoding or encoding a document.
#[derive(Debug)]
pub enum Error {
    /// The JSON wasn't valid or didn't fit the model.
    Json(serde_json::Error),
    /// The YAML wasn't valid or didn't fit the model.
    Yaml(serde_norway::Error),
    /// The document can't be handled by this library, e.g. its schema url names an older version.
    Unsupported(String),
}

impl std::fmt::Display for Error {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Error::Json(error) => write!(f, "invalid JSON document: {error}"),
            Error::Yaml(error) => write!(f, "invalid YAML document: {error}"),
            Error::Unsupported(message) => write!(f, "{message}"),
        }
    }
}

impl std::error::Error for Error {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Error::Json(error) => Some(error),
            Error::Yaml(error) => Some(error),
            Error::Unsupported(_) => None,
        }
    }
}
