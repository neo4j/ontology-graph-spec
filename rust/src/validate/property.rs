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

//! Shared logic for the property checks, used by both node and relationship validators.

use indexmap::IndexMap;

use crate::model::property::Property;

use super::{ptr, Issue};

/// The issue for a property without a name, or `None` when it has one.
///
/// A blank name counts as missing.
pub(crate) fn empty_property_name(
    property_id: &str,
    property: &Property,
    entity_path: [&str; 2],
) -> Option<Issue> {
    let name = property.name.as_deref();
    if name.is_none_or(|name| name.trim().is_empty()) {
        return Some(Issue::new(
            "missing_property_name",
            format!("Missing name for property '{property_id}'"),
            ptr![
                entity_path[0],
                entity_path[1],
                "properties",
                property_id,
                "name"
            ],
        ));
    }
    None
}

/// The issue for a property whose name another property of the same entity also uses, or `None`.
///
/// Flags every property carrying a duplicated name.
pub(crate) fn duplicate_property_name(
    properties: &IndexMap<String, Property>,
    property_id: &str,
    property: &Property,
    entity_path: [&str; 2],
) -> Option<Issue> {
    let name = property.name.as_deref()?;
    if name.trim().is_empty() {
        return None;
    }
    let is_duplicate = properties
        .iter()
        .any(|(other_id, other)| other_id != property_id && other.name.as_deref() == Some(name));
    if is_duplicate {
        return Some(Issue::new(
            "duplicate_property_name",
            format!("Duplicate property name '{name}'"),
            ptr![
                entity_path[0],
                entity_path[1],
                "properties",
                property_id,
                "name"
            ],
        ));
    }
    None
}

/// The issue for a property that restricts its values with both a `pattern` and a `one_of` list,
/// or `None`.
pub(crate) fn pattern_one_of_conflict(
    property_id: &str,
    property: &Property,
    entity_path: [&str; 2],
) -> Option<Issue> {
    if !property.pattern.is_empty() && !property.one_of.is_empty() {
        return Some(Issue::new(
            "property_pattern_one_of_conflict",
            format!("Property '{property_id}' can't set both pattern and one_of"),
            ptr![entity_path[0], entity_path[1], "properties", property_id],
        ));
    }
    None
}
