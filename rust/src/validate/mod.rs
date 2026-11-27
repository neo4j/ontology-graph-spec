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

//! Checking a model for issues.
//!
//! A [validator](Validation) checks one rule and reports [issues](Issue) as it walks the model.
//! Validators come in [sets](ValidationSet): [`ValidationSet::core`] holds the rules every model
//! must follow, the other sets add stricter or more opinionated ones.
//!
//! ```no_run
//! # fn main() -> Result<(), Box<dyn std::error::Error>> {
//! use ontology_graph_spec::GraphSpec;
//! use ontology_graph_spec::validate::ValidationSet;
//!
//! let model = GraphSpec::decode("{}")?;
//! for issue in model.validate(&ValidationSet::all()) {
//!     println!("{}: {} ({})", issue.code, issue.message, issue.path.unwrap_or_default());
//! }
//! # Ok(())
//! # }
//! ```

pub(crate) mod constraint;
pub(crate) mod duplicate;
pub(crate) mod length;
pub(crate) mod mapping;
pub(crate) mod node;
pub(crate) mod node_constraint;
pub(crate) mod node_index;
pub(crate) mod node_mapping;
pub(crate) mod property;
pub(crate) mod relationship;
pub(crate) mod relationship_constraint;
pub(crate) mod relationship_index;
pub(crate) mod relationship_mapping;
pub(crate) mod schema_url;
pub(crate) mod table;

use std::collections::BTreeMap;
use std::sync::Arc;

use serde::{Deserialize, Serialize};

use crate::model::GraphModel;

/// One problem found in a model: what and where, with extra detail about the value at fault.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub struct Issue {
    /// What is wrong, e.g. `duplicate_node_alias`.
    pub code: String,
    /// A human-readable description of the problem.
    pub message: String,
    /// Where in the model, as an RFC 6901 JSON pointer, e.g. `/nodes/person/aliases/1`.
    pub path: Option<String>,
    /// Extra detail about the value at fault, e.g. which alias repeats.
    #[serde(default)]
    pub details: BTreeMap<String, String>,
}

impl Issue {
    /// An issue at a location in the model.
    pub fn new(code: &str, message: impl Into<String>, path: impl Into<String>) -> Self {
        Issue {
            code: code.to_owned(),
            message: message.into(),
            path: Some(path.into()),
            details: BTreeMap::new(),
        }
    }

    /// An issue at a location in the model, with extra detail about the value at fault.
    pub fn with_details(
        code: &str,
        message: impl Into<String>,
        path: impl Into<String>,
        details: impl IntoIterator<Item = (impl Into<String>, impl Into<String>)>,
    ) -> Self {
        Issue {
            details: details
                .into_iter()
                .map(|(key, value)| (key.into(), value.into()))
                .collect(),
            ..Issue::new(code, message, path)
        }
    }
}

impl std::fmt::Display for Issue {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match &self.path {
            Some(path) => write!(f, "{} at {}: {}", self.code, path, self.message),
            None => write!(f, "{}: {}", self.code, self.message),
        }
    }
}

impl std::error::Error for Issue {}

/// A segment of an RFC 6901 JSON pointer: a map key or a list index.
pub(crate) enum Seg<'a> {
    Key(&'a str),
    Idx(usize),
}

impl<'a> From<&'a str> for Seg<'a> {
    fn from(key: &'a str) -> Self {
        Seg::Key(key)
    }
}

impl<'a> From<&'a String> for Seg<'a> {
    fn from(key: &'a String) -> Self {
        Seg::Key(key)
    }
}

impl From<usize> for Seg<'_> {
    fn from(index: usize) -> Self {
        Seg::Idx(index)
    }
}

/// Builds an RFC 6901 JSON pointer from its segments, escaping `~` and `/` in keys.
pub(crate) fn json_pointer(segments: &[Seg<'_>]) -> String {
    fn escape_segment(pointer: &mut String, segment: &str) {
        pointer.push('/');
        pointer.push_str(&segment.replace('~', "~0").replace('/', "~1"));
    }
    let mut pointer = String::new();
    for segment in segments {
        match segment {
            Seg::Key(key) => escape_segment(&mut pointer, key),
            Seg::Idx(index) => escape_segment(&mut pointer, &index.to_string()),
        }
    }
    pointer
}

/// Builds a [`json_pointer`] from mixed keys and indices: `ptr!["nodes", node_id, "aliases", 1]`.
macro_rules! ptr {
    ($($segment:expr),* $(,)?) => {
        $crate::validate::json_pointer(&[$($crate::validate::Seg::from($segment)),*])
    };
}

pub(crate) use ptr;

/// One rule a model must follow. See the [sets](ValidationSet) for the rules to run together.
pub trait Validation: Send + Sync {
    /// The name of the validator, stable across implementations, used to order and deduplicate
    /// sets.
    fn name(&self) -> &'static str;

    /// The validators that must find no issues before this one runs, so it doesn't repeat or
    /// depend on what they already flagged.
    fn depends_on(&self) -> &'static [&'static str] {
        &[]
    }

    /// Checks the model, appending every issue it finds.
    fn validate(&self, model: &GraphModel, issues: &mut Vec<Issue>);
}

/// A set of validators to run against a model, and the union of the issues they find.
///
/// Validators that depend on another only run when that one found no issues, so a set reports each
/// problem once, at its most specific.
#[derive(Clone, Default)]
pub struct ValidationSet {
    pub(crate) validators: Vec<std::sync::Arc<dyn Validation>>,
}

impl ValidationSet {
    /// The rules a model must follow beyond its JSON Schema: the schema url is a released one,
    /// references resolve, constraint and index details are consistent, relationship counts make
    /// sense, text is within length limits and lists such as aliases, labels and index,
    /// constraint and mapping key properties don't repeat an entry.
    #[must_use]
    pub fn core() -> ValidationSet {
        ValidationSet {
            validators: vec![
                Arc::new(schema_url::SchemaUrl),
                Arc::new(node_constraint::NodeTypeConstraint),
                Arc::new(relationship_constraint::RelationshipTypeConstraint),
                Arc::new(node::NodeConstraints),
                Arc::new(relationship::RelationshipConstraints),
                Arc::new(node_index::NodeIndexesExists),
                Arc::new(node_index::NodeIndexOptions),
                Arc::new(relationship_index::RelationshipIndexes),
                Arc::new(relationship_index::RelationshipIndexOptions),
                Arc::new(node_constraint::NodeExistenceConstraint),
                Arc::new(relationship_constraint::RelationshipExistenceConstraint),
                Arc::new(relationship::RelationshipNodes),
                Arc::new(relationship::RelationshipTargetCount),
                Arc::new(node::NodePropertyPatternOneOf),
                Arc::new(relationship::RelationshipPropertyPatternOneOf),
                Arc::new(relationship_mapping::RelationshipMappingKey),
                Arc::new(length::StringLength),
                Arc::new(node::NodeAliasDuplicate),
                Arc::new(relationship::RelationshipAliasDuplicate),
                Arc::new(node::NodePropertyAliasDuplicate),
                Arc::new(relationship::RelationshipPropertyAliasDuplicate),
                Arc::new(node::NodeLabelDuplicate),
                Arc::new(node_index::NodeIndexLabelDuplicate),
                Arc::new(node_index::NodeIndexPropertyDuplicate),
                Arc::new(relationship_index::RelationshipIndexPropertyDuplicate),
                Arc::new(node_constraint::NodeConstraintPropertyDuplicate),
                Arc::new(relationship_constraint::RelationshipConstraintPropertyDuplicate),
                Arc::new(node_mapping::NodeMappingKeyDuplicate),
                Arc::new(relationship_mapping::RelationshipMappingKeyDuplicate),
                Arc::new(table::TableColumnSupportedDuplicate),
            ],
        }
    }

    /// Every node has an identifier label and every relationship a type and from and to nodes.
    #[must_use]
    pub fn labelled() -> ValidationSet {
        ValidationSet {
            validators: vec![
                Arc::new(node::NodeLabel),
                Arc::new(relationship::RelationshipType),
                Arc::new(relationship::RelationshipTargetNodes),
            ],
        }
    }

    /// Property names are set and unique within their node or relationship, indexes and
    /// constraints have properties, constraints aren't duplicated, composite constraints don't
    /// overlap an existence constraint, and index and constraint names are unique.
    #[must_use]
    pub fn consistent_schema() -> ValidationSet {
        ValidationSet {
            validators: vec![
                Arc::new(node::NodePropertyEmptyName),
                Arc::new(node::NodePropertyDuplicateName),
                Arc::new(relationship::RelationshipPropertyEmptyName),
                Arc::new(relationship::RelationshipPropertyDuplicateName),
                Arc::new(node_index::NodeIndexProperties),
                Arc::new(relationship_index::RelationshipIndexProperties),
                Arc::new(node_constraint::NodeConstraintProperties),
                Arc::new(node_constraint::NodeConstraintDuplicatePropertySet),
                Arc::new(relationship_constraint::RelationshipConstraintDuplicatePropertySet),
                Arc::new(node_constraint::NodeExistenceCompositeConflict),
                Arc::new(node_index::NodeIndexConstraintNameConflict),
            ],
        }
    }

    /// Node mappings have a key, mapping ids are unique and dependencies resolve without cycles,
    /// and data source table columns have a unique name and, outside local tables, a type.
    #[must_use]
    pub fn mappable() -> ValidationSet {
        ValidationSet {
            validators: vec![
                Arc::new(node_mapping::NodeMappingKey),
                Arc::new(mapping::MappingDependencies),
                Arc::new(table::TableColumnEmptyName),
                Arc::new(table::TableColumnDuplicateName),
                Arc::new(table::TableColumnType),
            ],
        }
    }

    /// Neo4j bulk import restrictions: labels and types don't contain invalid characters and node
    /// mapping keys and composite constraint properties must be valid types.
    #[must_use]
    pub fn bulk_importable() -> ValidationSet {
        ValidationSet {
            validators: vec![
                Arc::new(node::NodeLabelToken),
                Arc::new(relationship::RelationshipTypeToken),
                Arc::new(node_mapping::NodeMappingKeyType),
                Arc::new(node_constraint::NodeCompositeConstraintPropertyType),
            ],
        }
    }

    /// Opinionated checks the format doesn't require: every node has properties and a constraint,
    /// and no `UNIQUE` or `EXISTS` constraint repeats the properties of a `KEY` constraint.
    #[must_use]
    pub fn well_constrained() -> ValidationSet {
        ValidationSet {
            validators: vec![
                Arc::new(node::NodeProperties),
                Arc::new(node::NodeConstraintCoverage),
                Arc::new(node_constraint::NodeKeyOverlap),
                Arc::new(relationship_constraint::RelationshipKeyOverlap),
            ],
        }
    }

    /// Every built-in validator.
    #[must_use]
    pub fn all() -> ValidationSet {
        ValidationSet::core()
            .union(ValidationSet::labelled())
            .union(ValidationSet::consistent_schema())
            .union(ValidationSet::mappable())
            .union(ValidationSet::bulk_importable())
            .union(ValidationSet::well_constrained())
    }

    /// The set named as in the documents: `core`, `labelled`, `consistent_schema`, `mappable`,
    /// `bulk_importable`, `well_constrained` or `all`.
    #[must_use]
    pub fn by_name(name: &str) -> Option<ValidationSet> {
        match name {
            "core" => Some(ValidationSet::core()),
            "labelled" => Some(ValidationSet::labelled()),
            "consistent_schema" => Some(ValidationSet::consistent_schema()),
            "mappable" => Some(ValidationSet::mappable()),
            "bulk_importable" => Some(ValidationSet::bulk_importable()),
            "well_constrained" => Some(ValidationSet::well_constrained()),
            "all" => Some(ValidationSet::all()),
            _ => None,
        }
    }

    /// The set with one more validator appended, for composing sets beyond the built-in ones.
    /// A validator that [depends on](Validation::depends_on) another carries that dependency:
    /// name the dependency's `name()` in its set for it to run.
    #[must_use]
    pub fn with_validator(self, validator: impl Validation + 'static) -> ValidationSet {
        let mut validators = self.validators;
        validators.push(Arc::new(validator));
        ValidationSet { validators }
    }

    /// Every validator of both sets, without duplicates.
    #[must_use]
    pub fn union(self, other: ValidationSet) -> ValidationSet {
        let mut validators = self.validators;
        for validator in other.validators {
            if !validators
                .iter()
                .any(|existing| existing.name() == validator.name())
            {
                validators.push(validator);
            }
        }
        ValidationSet { validators }
    }

    /// The validators in the set, in the order they run.
    #[must_use]
    pub fn validators(&self) -> &[std::sync::Arc<dyn Validation>] {
        &self.validators
    }
}

/// Checks the model against a set of validators, returning every issue found.
#[must_use]
pub fn validate(model: &GraphModel, set: &ValidationSet) -> Vec<Issue> {
    let validators = &set.validators;

    // The roots are the validators with no dependencies; everything else only runs once the
    // validator it depends on found no issues.
    let mut roots = Vec::new();
    let mut children: BTreeMap<&str, Vec<usize>> = BTreeMap::new();
    for (index, validator) in validators.iter().enumerate() {
        let dependencies = validator.depends_on();
        if dependencies.is_empty() {
            roots.push(index);
        } else {
            for dependency in dependencies {
                children.entry(dependency).or_default().push(index);
            }
        }
    }

    fn validate_at(
        index: usize,
        validators: &[std::sync::Arc<dyn Validation>],
        children: &BTreeMap<&str, Vec<usize>>,
        model: &GraphModel,
        issues: &mut Vec<Issue>,
    ) {
        let validator = &validators[index];
        let start = issues.len();
        validator.validate(model, issues);
        if issues.len() > start {
            return;
        }
        if let Some(dependents) = children.get(validator.name()) {
            for &dependent in dependents {
                validate_at(dependent, validators, children, model, issues);
            }
        }
    }

    let mut issues = Vec::new();
    for root in roots {
        validate_at(root, validators, &children, model, &mut issues);
    }
    issues
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn builds_json_pointers() {
        assert_eq!(json_pointer(&[Seg::from("nodes")]), "/nodes");
        assert_eq!(
            json_pointer(&[
                Seg::from("nodes"),
                Seg::from(&"tilde~slash/both".to_owned()),
                Seg::from(12usize)
            ]),
            "/nodes/tilde~0slash~1both/12"
        );
    }

    #[test]
    fn every_dependency_edge_resolves_and_the_graph_is_acyclic() {
        // A depends_on name that matches no validator in its set makes the holder neither a root
        // nor a child, silently dropping it from the run; a cycle drops everything on it. The
        // built-in sets are constants, so this invariant is testable once, here.
        for set in [
            ValidationSet::core(),
            ValidationSet::labelled(),
            ValidationSet::consistent_schema(),
            ValidationSet::mappable(),
            ValidationSet::bulk_importable(),
            ValidationSet::well_constrained(),
            ValidationSet::all(),
        ] {
            let names: std::collections::HashSet<&str> =
                set.validators().iter().map(|v| v.name()).collect();
            let by_name: std::collections::HashMap<_, _> =
                set.validators().iter().map(|v| (v.name(), v)).collect();
            for validator in set.validators() {
                for dependency in validator.depends_on() {
                    assert!(
                        names.contains(dependency),
                        "{} depends on {}, which is not in its set",
                        validator.name(),
                        dependency
                    );
                }
            }
            for start in names.iter() {
                let mut chain = vec![*start];
                while let Some(current) = chain.last() {
                    let Some(validator) = by_name.get(current) else {
                        break;
                    };
                    match validator.depends_on().first() {
                        Some(dependency) => {
                            assert!(
                                !chain.contains(dependency),
                                "dependency cycle through {dependency}"
                            );
                            chain.push(dependency);
                        }
                        None => break,
                    }
                }
            }
        }
    }

    #[test]
    fn sets_have_no_duplicates() {
        let all = ValidationSet::all();
        let names: Vec<&str> = all.validators().iter().map(|v| v.name()).collect();
        let mut sorted = names.clone();
        sorted.sort_unstable();
        sorted.dedup();
        assert_eq!(names.len(), sorted.len(), "every validator appears once");
    }
}
