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

//! The validators that walk the indexes of nodes.

use crate::model::node::{Node, NodeIndex};
use crate::model::GraphModel;

use super::constraint::missing_property_issues;
use super::node::{node_walk, NodeValidation};
use super::{ptr, Issue, Validation};

pub(crate) struct NodeIndexesExists;

impl NodeValidation for NodeIndexesExists {
    fn validate_index(
        &self,
        _model: &GraphModel,
        node_id: &str,
        node: &Node,
        index_id: &str,
        index: &NodeIndex,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(missing_property_issues(
            &node.properties,
            &index.properties,
            "missing_node_index_property",
            |property| format!("Missing property with id '{property}' for node index '{index_id}'"),
            &ptr![
                "nodes",
                node_id,
                "extensions",
                "indexes",
                index_id,
                "properties"
            ],
        ));
    }
}

node_walk!(NodeIndexesExists);

pub(crate) struct NodeIndexOptions;

impl NodeValidation for NodeIndexOptions {
    fn validate_index(
        &self,
        _model: &GraphModel,
        node_id: &str,
        _node: &Node,
        index_id: &str,
        index: &NodeIndex,
        issues: &mut Vec<Issue>,
    ) {
        if let Some(options) = &index.options {
            if options.index_type() != index.r#type {
                issues.push(Issue::new(
                    "node_index_type_options_mismatch",
                    format!(
                        "Cannot use options type '{}' with node index '{index_id}' type '{}'",
                        options.index_type(),
                        index.r#type
                    ),
                    ptr![
                        "nodes",
                        node_id,
                        "extensions",
                        "indexes",
                        index_id,
                        "options"
                    ],
                ));
            }
        }
    }
}

node_walk!(NodeIndexOptions);

pub(crate) struct NodeIndexProperties;

impl NodeValidation for NodeIndexProperties {
    fn validate_index(
        &self,
        _model: &GraphModel,
        node_id: &str,
        _node: &Node,
        index_id: &str,
        index: &NodeIndex,
        issues: &mut Vec<Issue>,
    ) {
        if index.properties.is_empty() {
            issues.push(Issue::new(
                "missing_node_index_properties",
                format!("Index '{index_id}' on node '{node_id}' must have at least one property"),
                ptr![
                    "nodes",
                    node_id,
                    "extensions",
                    "indexes",
                    index_id,
                    "properties"
                ],
            ));
        }
    }
}

node_walk!(NodeIndexProperties);

pub(crate) struct NodeIndexPropertyDuplicate;

impl NodeValidation for NodeIndexPropertyDuplicate {
    fn validate_index(
        &self,
        _model: &GraphModel,
        node_id: &str,
        _node: &Node,
        index_id: &str,
        index: &NodeIndex,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(super::duplicate::duplicate_entry_issues(
            "duplicate_node_index_property",
            "property",
            &format!("index '{index_id}' on node '{node_id}'"),
            &index.properties,
            &ptr![
                "nodes",
                node_id,
                "extensions",
                "indexes",
                index_id,
                "properties"
            ],
        ));
    }
}

node_walk!(NodeIndexPropertyDuplicate);

pub(crate) struct NodeIndexLabelDuplicate;

impl NodeValidation for NodeIndexLabelDuplicate {
    fn validate_index(
        &self,
        _model: &GraphModel,
        node_id: &str,
        _node: &Node,
        index_id: &str,
        index: &NodeIndex,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(super::duplicate::duplicate_entry_issues(
            "duplicate_node_index_label",
            "label",
            &format!("index '{index_id}' on node '{node_id}'"),
            &index.labels,
            &ptr![
                "nodes",
                node_id,
                "extensions",
                "indexes",
                index_id,
                "labels"
            ],
        ));
    }
}

node_walk!(NodeIndexLabelDuplicate);

pub(crate) struct NodeIndexConstraintNameConflict;

impl Validation for NodeIndexConstraintNameConflict {
    fn name(&self) -> &'static str {
        stringify!(NodeIndexConstraintNameConflict)
    }

    // A model-wide analysis, so it drives itself instead of riding the per-node walk: the
    // duplicate-name set is computed once, not once per node.
    fn validate(&self, model: &GraphModel, issues: &mut Vec<Issue>) {
        let duplicate_names = duplicate_names(model);
        for (node_id, node) in &model.nodes {
            for (index_id, index) in &node.extensions.indexes {
                if let Some(name) = meaningful_name(index.name.as_deref()) {
                    if duplicate_names.contains(name) {
                        issues.push(Issue::new(
                            "duplicate_node_index_constraint_name",
                            format!(
                                "Index '{index_id}' on node '{node_id}' reuses name '{name}' \
                                 already used by another index or constraint"
                            ),
                            ptr!["nodes", node_id, "extensions", "indexes", index_id, "name"],
                        ));
                    }
                }
            }

            for (constraint_id, constraint) in &node.constraints {
                if let Some(name) = meaningful_name(constraint.name.as_deref()) {
                    if duplicate_names.contains(name) {
                        issues.push(Issue::new(
                            "duplicate_node_index_constraint_name",
                            format!(
                                "Constraint '{constraint_id}' on node '{node_id}' reuses name \
                                 '{name}' already used by another index or constraint"
                            ),
                            ptr!["nodes", node_id, "constraints", constraint_id, "name"],
                        ));
                    }
                }
            }
        }
    }
}

/// A name that is set and not blank: the names worth comparing. `None` for missing or blank.
fn meaningful_name(name: Option<&str>) -> Option<&str> {
    match name {
        Some(name) if !name.trim().is_empty() => Some(name),
        _ => None,
    }
}

/// The names used by more than one node index or constraint across the whole model. Missing
/// and blank names are skipped.
fn duplicate_names(model: &GraphModel) -> std::collections::HashSet<&str> {
    let mut seen: std::collections::HashMap<&str, usize> = std::collections::HashMap::new();
    for node in model.nodes.values() {
        for index in node.extensions.indexes.values() {
            if let Some(name) = meaningful_name(index.name.as_deref()) {
                *seen.entry(name).or_default() += 1;
            }
        }
        for constraint in node.constraints.values() {
            if let Some(name) = meaningful_name(constraint.name.as_deref()) {
                *seen.entry(name).or_default() += 1;
            }
        }
    }
    seen.into_iter()
        .filter(|(_, count)| *count > 1)
        .map(|(name, _)| name)
        .collect()
}
