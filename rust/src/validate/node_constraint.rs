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

//! The validators that walk the constraints of nodes.

use crate::model::node::{Node, NodeConstraint};
use crate::model::types::ConstraintType;
use crate::model::GraphModel;

use super::constraint::{
    duplicate_constraint_ids, duplicate_property_set_issue, has_single_distinct_property,
    is_composite_constraint, redundant_key_overlap_issues,
};
use super::duplicate::duplicate_entry_issues;
use super::node::{node_walk, NodeValidation};
use super::{ptr, Issue, Validation};

pub(crate) struct NodeConstraintProperties;

impl NodeValidation for NodeConstraintProperties {
    fn validate_constraint(
        &self,
        _model: &GraphModel,
        node_id: &str,
        _node: &Node,
        constraint_id: &str,
        constraint: &NodeConstraint,
        issues: &mut Vec<Issue>,
    ) {
        if constraint.properties.is_empty() {
            issues.push(Issue::new(
                "missing_node_constraint_properties",
                format!("Constraint '{constraint_id}' on node '{node_id}' has no properties"),
                ptr!["nodes", node_id, "constraints", constraint_id, "properties"],
            ));
        }
    }
}

node_walk!(NodeConstraintProperties);

pub(crate) struct NodeConstraintDuplicatePropertySet;

impl NodeValidation for NodeConstraintDuplicatePropertySet {
    fn validate_constraint(
        &self,
        _model: &GraphModel,
        node_id: &str,
        node: &Node,
        constraint_id: &str,
        constraint: &NodeConstraint,
        issues: &mut Vec<Issue>,
    ) {
        let duplicates = duplicate_constraint_ids(constraint_id, constraint, &node.constraints);
        if !duplicates.is_empty() {
            issues.push(duplicate_property_set_issue(
                constraint_id,
                constraint,
                &duplicates,
                node_id,
                "node",
                "duplicate_node_constraint_property_set",
                "nodes",
            ));
        }
    }
}

node_walk!(NodeConstraintDuplicatePropertySet);

pub(crate) struct NodeConstraintPropertyDuplicate;

impl NodeValidation for NodeConstraintPropertyDuplicate {
    fn validate_constraint(
        &self,
        _model: &GraphModel,
        node_id: &str,
        _node: &Node,
        constraint_id: &str,
        constraint: &NodeConstraint,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(duplicate_entry_issues(
            "duplicate_node_constraint_property",
            "property",
            &format!("constraint '{constraint_id}' on node '{node_id}'"),
            &constraint.properties,
            &ptr!["nodes", node_id, "constraints", constraint_id, "properties"],
        ));
    }
}

node_walk!(NodeConstraintPropertyDuplicate);

pub(crate) struct NodeExistenceConstraint;

impl NodeValidation for NodeExistenceConstraint {
    fn validate_constraint(
        &self,
        _model: &GraphModel,
        node_id: &str,
        _node: &Node,
        constraint_id: &str,
        constraint: &NodeConstraint,
        issues: &mut Vec<Issue>,
    ) {
        if constraint.r#type == ConstraintType::Exists
            && !has_single_distinct_property(&constraint.properties)
        {
            issues.push(Issue::new(
                "invalid_node_exist_constraint_property_count",
                format!(
                    "Node existence constraint '{constraint_id}' must have exactly one property."
                ),
                ptr!["nodes", node_id, "constraints", constraint_id, "properties"],
            ));
        }
    }
}

node_walk!(NodeExistenceConstraint, &["NodeConstraints"]);

pub(crate) struct NodeTypeConstraint;

impl NodeValidation for NodeTypeConstraint {
    fn validate_constraint(
        &self,
        _model: &GraphModel,
        node_id: &str,
        _node: &Node,
        constraint_id: &str,
        constraint: &NodeConstraint,
        issues: &mut Vec<Issue>,
    ) {
        if constraint.r#type == ConstraintType::PropertyType
            && !has_single_distinct_property(&constraint.properties)
        {
            issues.push(Issue::new(
                "invalid_node_type_constraint_property_count",
                format!("Node type constraint '{constraint_id}' must have exactly one property."),
                ptr!["nodes", node_id, "constraints", constraint_id, "properties"],
            ));
        }
    }
}

node_walk!(NodeTypeConstraint, &["NodeConstraints"]);

pub(crate) struct NodeExistenceCompositeConflict;

impl NodeValidation for NodeExistenceCompositeConflict {
    fn validate_node(
        &self,
        _model: &GraphModel,
        node_id: &str,
        node: &Node,
        issues: &mut Vec<Issue>,
    ) {
        let composite_constraints: Vec<&NodeConstraint> = node
            .constraints
            .values()
            .filter(|constraint| {
                constraint.r#type != ConstraintType::Exists
                    && is_composite_constraint(&constraint.properties)
            })
            .collect();
        for (constraint_id, constraint) in &node.constraints {
            if constraint.r#type != ConstraintType::Exists {
                continue;
            }
            if !has_single_distinct_property(&constraint.properties) {
                continue;
            }
            let property_id = &constraint.properties[0];
            let conflicts_with_composite = composite_constraints
                .iter()
                .any(|other| other.properties.contains(property_id));
            if conflicts_with_composite {
                issues.push(Issue::new(
                    "node_existence_composite_conflict",
                    format!(
                        "Existence constraint '{constraint_id}' on property '{property_id}' \
                         conflicts with a composite constraint on node '{node_id}'"
                    ),
                    ptr!["nodes", node_id, "constraints", constraint_id],
                ));
            }
        }
    }
}

node_walk!(NodeExistenceCompositeConflict);

pub(crate) struct NodeKeyOverlap;

impl NodeValidation for NodeKeyOverlap {
    fn validate_node(
        &self,
        _model: &GraphModel,
        node_id: &str,
        node: &Node,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(redundant_key_overlap_issues(
            node_id,
            &node.constraints,
            "nodes",
            "node",
            "redundant_node_constraint_key_overlap",
        ));
    }
}

node_walk!(NodeKeyOverlap);

pub(crate) struct NodeCompositeConstraintPropertyType;

impl NodeValidation for NodeCompositeConstraintPropertyType {
    fn validate_constraint(
        &self,
        _model: &GraphModel,
        node_id: &str,
        node: &Node,
        constraint_id: &str,
        constraint: &NodeConstraint,
        issues: &mut Vec<Issue>,
    ) {
        if !is_composite_constraint(&constraint.properties) {
            return;
        }
        let has_invalid_type = constraint.properties.iter().any(|property_id| {
            let Some(property) = node.properties.get(property_id) else {
                return false;
            };
            !property.r#type.is_bulk_importable_key_type()
        });
        if has_invalid_type {
            issues.push(Issue::new(
                "invalid_node_composite_constraint_property_type",
                format!(
                    "Composite constraint '{constraint_id}' on node '{node_id}' \
                     properties must be STRING or INTEGER for bulk import"
                ),
                ptr!["nodes", node_id, "constraints", constraint_id],
            ));
        }
    }
}

node_walk!(NodeCompositeConstraintPropertyType);
