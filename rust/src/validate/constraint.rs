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

//! Shared logic for constraint checks: what makes a constraint composite, and which constraints
//! duplicate which.

use std::collections::{BTreeSet, HashSet};

use indexmap::IndexMap;

use crate::model::constraint::Constraint;
use crate::model::property::Property;

use super::{ptr, Issue};

/// Whether a constraint covers two or more distinct properties.
pub(crate) fn is_composite_constraint(properties: &[String]) -> bool {
    properties.iter().collect::<HashSet<_>>().len() >= 2
}

/// The ids of the other constraints that duplicate `constraint_id`.
///
/// Duplicates are constraints on the same properties with the same type or, when composite, any
/// type.
pub(crate) fn duplicate_constraint_ids(
    constraint_id: &str,
    constraint: &Constraint,
    constraints: &IndexMap<String, Constraint>,
) -> Vec<String> {
    let properties: HashSet<&String> = constraint.properties.iter().collect();
    let composite = is_composite_constraint(&constraint.properties);
    constraints
        .iter()
        .filter(|(other_id, other)| {
            other_id.as_str() != constraint_id
                && other.properties.iter().collect::<HashSet<_>>() == properties
                && (other.r#type == constraint.r#type || composite)
        })
        .map(|(other_id, _)| other_id.clone())
        .collect()
}

/// Whether a constraint covers exactly one distinct property, as EXISTS and PROPERTY_TYPE
/// constraints must. The count is of distinct entries: a repeated single property still passes,
/// which the duplicate-property validator flags separately.
pub(crate) fn has_single_distinct_property(properties: &[String]) -> bool {
    properties.iter().collect::<HashSet<_>>().len() == 1
}

/// The issues for a constraint's or index's property references that don't resolve in the
/// entity's own properties: one per unresolved reference, in list order, at the reference's
/// index. Shared by the node and relationship, constraint and index validators.
pub(crate) fn missing_property_issues(
    properties: &IndexMap<String, Property>,
    references: &[String],
    code: &str,
    describe: impl Fn(&str) -> String,
    base_path: &str,
) -> Vec<Issue> {
    references
        .iter()
        .enumerate()
        .filter(|(_, reference)| !properties.contains_key(*reference))
        .map(|(index, reference)| {
            Issue::new(code, describe(reference), format!("{base_path}/{index}"))
        })
        .collect()
}

/// The issues for a `UNIQUE` or `EXISTS` constraint whose property set a `KEY` constraint
/// already covers on the same entity: redundant next to the key. Shared by the node and
/// relationship validators.
pub(crate) fn redundant_key_overlap_issues(
    entity_id: &str,
    constraints: &IndexMap<String, Constraint>,
    entity_root: &str,
    entity_word: &str,
    code: &str,
) -> Vec<Issue> {
    let key_property_sets: HashSet<BTreeSet<&str>> = constraints
        .values()
        .filter(|constraint| constraint.r#type == crate::model::types::ConstraintType::Key)
        .map(|constraint| {
            constraint
                .properties
                .iter()
                .map(String::as_str)
                .collect::<BTreeSet<_>>()
        })
        .collect();
    if key_property_sets.is_empty() {
        return Vec::new();
    }
    constraints
        .iter()
        .filter(|(_, constraint)| {
            matches!(
                constraint.r#type,
                crate::model::types::ConstraintType::Unique
                    | crate::model::types::ConstraintType::Exists
            ) && key_property_sets.contains(
                &constraint
                    .properties
                    .iter()
                    .map(String::as_str)
                    .collect::<BTreeSet<_>>(),
            )
        })
        .map(|(constraint_id, constraint)| {
            Issue::new(
                code,
                format!(
                    "Constraint '{constraint_id}' ({type}) is redundant - a KEY constraint \
                     already covers the same properties on {entity_word} '{entity_id}'",
                    type = constraint.r#type
                ),
                ptr![entity_root, entity_id, "constraints", constraint_id],
            )
        })
        .collect()
}

/// The issue for a constraint whose property set another constraint of the same entity already
/// covers, with the duplicated ids in the message. Shared by the node and relationship
/// validators.
pub(crate) fn duplicate_property_set_issue(
    constraint_id: &str,
    constraint: &Constraint,
    duplicates: &[String],
    entity_id: &str,
    entity_word: &str,
    code: &str,
    entity_root: &str,
) -> Issue {
    let properties = constraint.properties.join(", ");
    let duplicated = duplicates
        .iter()
        .map(|duplicate| format!("'{duplicate}'"))
        .collect::<Vec<_>>()
        .join(", ");
    Issue::new(
        code,
        format!(
            "Constraint '{constraint_id}' ({type} on {properties}) on {entity_word} '{entity_id}' \
             duplicates {duplicated}",
            type = constraint.r#type
        ),
        ptr![
            entity_root,
            entity_id,
            "constraints",
            constraint_id,
            "properties"
        ],
    )
}
