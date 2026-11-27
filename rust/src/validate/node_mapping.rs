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

//! The validators that walk the node mappings of a model's data sources.

use crate::model::mapping::Mapping;
use crate::model::node::Node;
use crate::model::GraphModel;

use super::duplicate::duplicate_entry_issues;
use super::node::{node_walk, NodeValidation};
use super::{ptr, Issue, Validation};

/// A node mapping and where it lives in the model's mappings.
pub(crate) struct LocatedNodeMapping<'a> {
    pub list_id: &'a str,
    pub index: usize,
    pub mapping: &'a crate::model::mapping::NodeMapping,
}

/// Finds every mapping of `node_id` along with where each lives in the model's mappings.
pub(crate) fn find_node_mappings<'a>(
    model: &'a GraphModel,
    node_id: &str,
) -> Vec<LocatedNodeMapping<'a>> {
    let mut found = Vec::new();
    for (list_id, mappings) in &model.extensions.data_source_mappings {
        for (index, mapping) in mappings.iter().enumerate() {
            if let Mapping::Node(mapping) = mapping {
                if mapping.node == node_id {
                    found.push(LocatedNodeMapping {
                        list_id,
                        index,
                        mapping,
                    });
                }
            }
        }
    }
    found
}

pub(crate) struct NodeMappingKey;

impl NodeValidation for NodeMappingKey {
    fn validate_node(
        &self,
        model: &GraphModel,
        node_id: &str,
        _node: &Node,
        issues: &mut Vec<Issue>,
    ) {
        for located in find_node_mappings(model, node_id) {
            if located.mapping.key.is_empty() {
                issues.push(Issue::new(
                    "missing_node_mapping_key",
                    format!("Node '{node_id}' has no mapping key defined"),
                    ptr![
                        "extensions",
                        "data_source_mappings",
                        located.list_id,
                        located.index,
                        "key"
                    ],
                ));
            }
        }
    }
}

node_walk!(NodeMappingKey);

pub(crate) struct NodeMappingKeyDuplicate;

impl NodeValidation for NodeMappingKeyDuplicate {
    fn validate_node(
        &self,
        model: &GraphModel,
        node_id: &str,
        _node: &Node,
        issues: &mut Vec<Issue>,
    ) {
        for located in find_node_mappings(model, node_id) {
            issues.extend(duplicate_entry_issues(
                "duplicate_node_mapping_key",
                "mapping key",
                &format!("node '{node_id}'"),
                &located.mapping.key,
                &ptr![
                    "extensions",
                    "data_source_mappings",
                    located.list_id,
                    located.index,
                    "key"
                ],
            ));
        }
    }
}

node_walk!(NodeMappingKeyDuplicate);

pub(crate) struct NodeMappingKeyType;

impl NodeValidation for NodeMappingKeyType {
    fn validate_node(
        &self,
        model: &GraphModel,
        node_id: &str,
        node: &Node,
        issues: &mut Vec<Issue>,
    ) {
        for located in find_node_mappings(model, node_id) {
            for (key_index, property_id) in located.mapping.key.iter().enumerate() {
                let Some(property) = node.properties.get(property_id) else {
                    continue;
                };
                if !property.r#type.is_bulk_importable_key_type() {
                    issues.push(Issue::new(
                        "invalid_node_mapping_key_type",
                        format!("Mapping key '{property_id}' on node '{node_id}' must be STRING or INTEGER"),
                        ptr![
                            "extensions",
                            "data_source_mappings",
                            located.list_id,
                            located.index,
                            "key",
                            key_index
                        ],
                    ));
                }
            }
        }
    }
}

node_walk!(NodeMappingKeyType);
