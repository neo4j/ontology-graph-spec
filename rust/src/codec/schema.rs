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

//! Validation of documents against the published JSON Schema of the format.

/// The published JSON Schema of the format. A symlink to the repository's own copy, so there is
/// one source of truth; packaging dereferences it, so the published crate carries the content.
/// A unit test checks the link resolves to the repository's copy (and catches a checkout that
/// materialized it as a plain text file, which some Windows configurations do).
#[cfg(feature = "schema-validation")]
const SCHEMA: &str = include_str!("../../ontology-graph-spec.schema.json");

/// Checks a document against the published JSON Schema, returning one message per violation.
///
/// The structural gate: closed objects and enums, required fields, types. The validator sets in
/// [`crate::validate`] are the semantic checks beyond it.
#[cfg(feature = "schema-validation")]
pub(crate) fn validate_document(value: &serde_json::Value) -> Result<(), SchemaViolations> {
    static VALIDATOR: std::sync::OnceLock<jsonschema::Validator> = std::sync::OnceLock::new();
    let validator = VALIDATOR.get_or_init(|| {
        let schema: serde_json::Value =
            serde_json::from_str(SCHEMA).expect("the published schema is valid JSON");
        jsonschema::validator_for(&schema).expect("the published schema compiles")
    });
    let mut errors = Vec::new();
    for error in validator.iter_errors(value) {
        errors.push(format!("{} at {}", error, error.instance_path));
    }
    if errors.is_empty() {
        Ok(())
    } else {
        Err(SchemaViolations(errors))
    }
}

/// The ways a document violates the published JSON Schema, from
/// [`validate_document_schema`](crate::validate_document_schema). One line per violation, each
/// naming where in the document; the list is the field, for callers that report each violation
/// in its own place.
#[cfg(feature = "schema-validation")]
#[derive(Clone, Debug, PartialEq)]
pub struct SchemaViolations(pub Vec<String>);

#[cfg(feature = "schema-validation")]
impl std::fmt::Display for SchemaViolations {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "the document violates the published JSON Schema:")?;
        for violation in &self.0 {
            write!(f, "\n  {violation}")?;
        }
        Ok(())
    }
}

#[cfg(feature = "schema-validation")]
impl std::error::Error for SchemaViolations {}

#[cfg(feature = "schema-validation")]
impl SchemaViolations {
    /// The violations, one line each, each naming where in the document.
    #[must_use]
    pub fn violations(&self) -> &[String] {
        &self.0
    }
}

#[cfg(all(test, feature = "schema-validation"))]
mod tests {
    use super::*;

    /// The schema is a symlink to the repository's own copy: inside the repository the two must
    /// resolve to the same content, and a checkout that materialized the link as a plain text
    /// file — which some Windows configurations do — fails here instead of at first use. From a
    /// published tarball there is no repository copy, so the check stands down. Test binaries
    /// run from the crate root, where the repository copy is one level up.
    #[test]
    fn the_schema_resolves_to_the_repositories_copy() {
        let Ok(repository_schema) = std::fs::read("../ontology-graph-spec.schema.json") else {
            return;
        };
        assert_eq!(
            SCHEMA.as_bytes(),
            repository_schema.as_slice(),
            "rust/ontology-graph-spec.schema.json does not resolve to the repository's schema: \
             if this is a Windows checkout, enable symlink support (Developer Mode) and re-clone"
        );
    }

    #[test]
    fn the_schema_compiles_and_validates() {
        let value: serde_json::Value = serde_json::from_str(
            r#"{"schema":"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json","version":"1","id":"t"}"#,
        )
        .unwrap();
        assert!(validate_document(&value).is_ok());
    }
}
