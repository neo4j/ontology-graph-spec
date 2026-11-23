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

//! The validators that walk nodes: labels, aliases, properties, constraints, indexes and node
//! mappings.

use crate::model::node::{Node, NodeConstraint, NodeIndex};
use crate::model::property::Property;
use crate::model::GraphModel;

use super::constraint::missing_property_issues;
use super::duplicate::duplicate_entry_issues;
use super::property::{duplicate_property_name, empty_property_name, pattern_one_of_conflict};
use super::{ptr, Issue, Validation};

/// The parts of a node a validator can check, mirroring the reference implementation's
/// node-validation interface: validators override what they need, and the shared walk visits
/// every node, its properties, its constraints and its indexes in that order.
pub(crate) trait NodeValidation: Validation {
    fn validate_node(
        &self,
        _model: &GraphModel,
        _node_id: &str,
        _node: &Node,
        _issues: &mut Vec<Issue>,
    ) {
    }

    fn validate_property(
        &self,
        _model: &GraphModel,
        _node_id: &str,
        _node: &Node,
        _property_id: &str,
        _property: &Property,
        _issues: &mut Vec<Issue>,
    ) {
    }

    fn validate_constraint(
        &self,
        _model: &GraphModel,
        _node_id: &str,
        _node: &Node,
        _constraint_id: &str,
        _constraint: &NodeConstraint,
        _issues: &mut Vec<Issue>,
    ) {
    }

    fn validate_index(
        &self,
        _model: &GraphModel,
        _node_id: &str,
        _node: &Node,
        _index_id: &str,
        _index: &NodeIndex,
        _issues: &mut Vec<Issue>,
    ) {
    }
}

/// Implements the [Validation] walk for a [NodeValidation].
macro_rules! node_walk {
    ($validator:ty) => {
        node_walk!($validator, &[]);
    };
    ($validator:ty, $depends:expr) => {
        impl Validation for $validator {
            fn name(&self) -> &'static str {
                stringify!($validator)
            }

            fn depends_on(&self) -> &'static [&'static str] {
                $depends
            }

            fn validate(&self, model: &GraphModel, issues: &mut Vec<Issue>) {
                for (node_id, node) in &model.nodes {
                    self.validate_node(model, node_id, node, issues);
                    for (property_id, property) in &node.properties {
                        self.validate_property(model, node_id, node, property_id, property, issues);
                    }
                    for (constraint_id, constraint) in &node.constraints {
                        self.validate_constraint(
                            model,
                            node_id,
                            node,
                            constraint_id,
                            constraint,
                            issues,
                        );
                    }
                    for (index_id, index) in &node.extensions.indexes {
                        self.validate_index(model, node_id, node, index_id, index, issues);
                    }
                }
            }
        }
    };
}

pub(crate) use node_walk;

pub(crate) struct NodeLabel;

impl NodeValidation for NodeLabel {
    fn validate_node(
        &self,
        _model: &GraphModel,
        node_id: &str,
        node: &Node,
        issues: &mut Vec<Issue>,
    ) {
        let identifier = node.labels.identifier.as_deref();
        if identifier.is_none_or(|label| label.trim().is_empty()) {
            issues.push(Issue::new(
                "missing_node_identifier_label",
                format!("Missing identifier label for node '{node_id}'"),
                ptr!["nodes", node_id, "labels", "identifier"],
            ));
        }
    }
}

node_walk!(NodeLabel);

pub(crate) struct NodeLabelToken;

impl NodeValidation for NodeLabelToken {
    fn validate_node(
        &self,
        _model: &GraphModel,
        node_id: &str,
        node: &Node,
        issues: &mut Vec<Issue>,
    ) {
        if let Some(token) = node.labels.identifier.as_deref() {
            if token.contains(':') || token.contains('=') {
                issues.push(Issue::new(
                    "invalid_node_label_token",
                    format!(
                        "Node label '{token}' contains invalid characters (: or =) for bulk import"
                    ),
                    ptr!["nodes", node_id, "labels", "identifier"],
                ));
            }
        }
    }
}

node_walk!(NodeLabelToken);

pub(crate) struct NodeLabelDuplicate;

impl NodeValidation for NodeLabelDuplicate {
    fn validate_node(
        &self,
        _model: &GraphModel,
        node_id: &str,
        node: &Node,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(duplicate_entry_issues(
            "duplicate_node_implied_label",
            "implied label",
            &format!("node '{node_id}'"),
            &node.labels.implied,
            &ptr!["nodes", node_id, "labels", "implied"],
        ));
        issues.extend(duplicate_entry_issues(
            "duplicate_node_optional_label",
            "optional label",
            &format!("node '{node_id}'"),
            &node.labels.optional,
            &ptr!["nodes", node_id, "labels", "optional"],
        ));
    }
}

node_walk!(NodeLabelDuplicate);

pub(crate) struct NodeAliasDuplicate;

impl NodeValidation for NodeAliasDuplicate {
    fn validate_node(
        &self,
        _model: &GraphModel,
        node_id: &str,
        node: &Node,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(duplicate_entry_issues(
            "duplicate_node_alias",
            "alias",
            &format!("node '{node_id}'"),
            &node.aliases,
            &ptr!["nodes", node_id, "aliases"],
        ));
    }
}

node_walk!(NodeAliasDuplicate);

pub(crate) struct NodeProperties;

impl NodeValidation for NodeProperties {
    fn validate_node(
        &self,
        _model: &GraphModel,
        node_id: &str,
        node: &Node,
        issues: &mut Vec<Issue>,
    ) {
        if node.properties.is_empty() {
            issues.push(Issue::new(
                "missing_node_properties",
                format!("Node '{node_id}' has no properties"),
                ptr!["nodes", node_id, "properties"],
            ));
        }
    }
}

node_walk!(NodeProperties);

pub(crate) struct NodePropertyAliasDuplicate;

impl NodeValidation for NodePropertyAliasDuplicate {
    fn validate_property(
        &self,
        _model: &GraphModel,
        node_id: &str,
        _node: &Node,
        property_id: &str,
        property: &Property,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(duplicate_entry_issues(
            "duplicate_node_property_alias",
            "alias",
            &format!("property '{property_id}' of node '{node_id}'"),
            &property.aliases,
            &ptr!["nodes", node_id, "properties", property_id, "aliases"],
        ));
    }
}

node_walk!(NodePropertyAliasDuplicate);

pub(crate) struct NodePropertyDuplicateName;

impl NodeValidation for NodePropertyDuplicateName {
    fn validate_property(
        &self,
        _model: &GraphModel,
        node_id: &str,
        node: &Node,
        property_id: &str,
        property: &Property,
        issues: &mut Vec<Issue>,
    ) {
        if let Some(issue) =
            duplicate_property_name(&node.properties, property_id, property, ["nodes", node_id])
        {
            issues.push(issue);
        }
    }
}

node_walk!(NodePropertyDuplicateName);

pub(crate) struct NodePropertyEmptyName;

impl NodeValidation for NodePropertyEmptyName {
    fn validate_property(
        &self,
        _model: &GraphModel,
        node_id: &str,
        _node: &Node,
        property_id: &str,
        property: &Property,
        issues: &mut Vec<Issue>,
    ) {
        if let Some(issue) = empty_property_name(property_id, property, ["nodes", node_id]) {
            issues.push(issue);
        }
    }
}

node_walk!(NodePropertyEmptyName);

pub(crate) struct NodePropertyPatternOneOf;

impl NodeValidation for NodePropertyPatternOneOf {
    fn validate_property(
        &self,
        _model: &GraphModel,
        node_id: &str,
        _node: &Node,
        property_id: &str,
        property: &Property,
        issues: &mut Vec<Issue>,
    ) {
        if let Some(issue) = pattern_one_of_conflict(property_id, property, ["nodes", node_id]) {
            issues.push(issue);
        }
    }
}

node_walk!(NodePropertyPatternOneOf);

pub(crate) struct NodeConstraints;

impl NodeValidation for NodeConstraints {
    fn validate_constraint(
        &self,
        _model: &GraphModel,
        node_id: &str,
        node: &Node,
        constraint_id: &str,
        constraint: &NodeConstraint,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(missing_property_issues(
            &node.properties,
            &constraint.properties,
            "missing_node_constraint_property",
            |property| {
                format!(
                    "Missing property with id '{property}' for node constraint '{constraint_id}'"
                )
            },
            &ptr!["nodes", node_id, "constraints", constraint_id, "properties"],
        ));
    }
}

node_walk!(NodeConstraints);

pub(crate) struct NodeConstraintCoverage;

impl NodeValidation for NodeConstraintCoverage {
    fn validate_node(
        &self,
        _model: &GraphModel,
        node_id: &str,
        node: &Node,
        issues: &mut Vec<Issue>,
    ) {
        if node.constraints.is_empty() {
            issues.push(Issue::new(
                "missing_node_constraint_coverage",
                format!("Node '{node_id}' has no constraints"),
                ptr!["nodes", node_id, "constraints"],
            ));
        }
    }
}

node_walk!(NodeConstraintCoverage);
