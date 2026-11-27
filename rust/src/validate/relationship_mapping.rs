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

//! The validators that walk the relationship mappings of a model's data sources.

use crate::model::mapping::Mapping;
use crate::model::relationship::Relationship;
use crate::model::GraphModel;

use super::duplicate::duplicate_entry_issues;
use super::relationship::{relationship_walk, RelationshipValidation};
use super::{ptr, Issue, Validation};

/// A relationship mapping and where it lives in the model's mappings.
pub(crate) struct LocatedRelationshipMapping<'a> {
    pub list_id: &'a str,
    pub index: usize,
    pub mapping: &'a crate::model::mapping::RelationshipMapping,
}

/// Finds every mapping of `relationship_id` along with where each lives in the model's mappings.
pub(crate) fn find_relationship_mappings<'a>(
    model: &'a GraphModel,
    relationship_id: &str,
) -> Vec<LocatedRelationshipMapping<'a>> {
    let mut found = Vec::new();
    for (list_id, mappings) in &model.extensions.data_source_mappings {
        for (index, mapping) in mappings.iter().enumerate() {
            if let Mapping::Relationship(mapping) = mapping {
                if mapping.relationship == relationship_id {
                    found.push(LocatedRelationshipMapping {
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

pub(crate) struct RelationshipMappingKey;

impl RelationshipValidation for RelationshipMappingKey {
    fn validate_relationship(
        &self,
        model: &GraphModel,
        relationship_id: &str,
        relationship: &Relationship,
        issues: &mut Vec<Issue>,
    ) {
        for located in find_relationship_mappings(model, relationship_id) {
            for (key_index, property_id) in located.mapping.key.iter().enumerate() {
                if relationship.properties.contains_key(property_id) {
                    continue;
                }
                issues.push(Issue::with_details(
                    "invalid_relationship_mapping_key",
                    format!(
                        "Mapping key '{property_id}' is not a property of relationship \
                         '{relationship_id}'"
                    ),
                    ptr![
                        "extensions",
                        "data_source_mappings",
                        located.list_id,
                        located.index,
                        "key",
                        key_index
                    ],
                    [
                        ("relationship", relationship_id.to_owned()),
                        ("property", property_id.clone()),
                    ],
                ));
            }
        }
    }
}

relationship_walk!(RelationshipMappingKey);

pub(crate) struct RelationshipMappingKeyDuplicate;

impl RelationshipValidation for RelationshipMappingKeyDuplicate {
    fn validate_relationship(
        &self,
        model: &GraphModel,
        relationship_id: &str,
        _relationship: &Relationship,
        issues: &mut Vec<Issue>,
    ) {
        for located in find_relationship_mappings(model, relationship_id) {
            issues.extend(duplicate_entry_issues(
                "duplicate_relationship_mapping_key",
                "mapping key",
                &format!("relationship '{relationship_id}'"),
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

relationship_walk!(RelationshipMappingKeyDuplicate);
