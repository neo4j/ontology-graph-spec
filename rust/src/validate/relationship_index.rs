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

//! The validators that walk the indexes of relationships.

use crate::model::relationship::{Relationship, RelationshipIndex};
use crate::model::GraphModel;

use super::constraint::missing_property_issues;
use super::duplicate::duplicate_entry_issues;
use super::relationship::{relationship_walk, RelationshipValidation};
use super::{ptr, Issue, Validation};

pub(crate) struct RelationshipIndexes;

impl RelationshipValidation for RelationshipIndexes {
    fn validate_index(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        relationship: &Relationship,
        index_id: &str,
        index: &RelationshipIndex,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(missing_property_issues(
            &relationship.properties,
            &index.properties,
            "missing_relationship_index_property",
            |property| {
                format!("Missing property with id '{property}' for relationship index '{index_id}'")
            },
            &ptr![
                "relationships",
                relationship_id,
                "extensions",
                "indexes",
                index_id,
                "properties"
            ],
        ));
    }
}

relationship_walk!(RelationshipIndexes);

pub(crate) struct RelationshipIndexOptions;

impl RelationshipValidation for RelationshipIndexOptions {
    fn validate_index(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        _relationship: &Relationship,
        index_id: &str,
        index: &RelationshipIndex,
        issues: &mut Vec<Issue>,
    ) {
        if let Some(options) = &index.options {
            if options.index_type() != index.r#type {
                issues.push(Issue::new(
                    "relationship_index_type_options_mismatch",
                    format!(
                        "Cannot use options type '{}' with relationship index '{index_id}' type \
                         '{}'",
                        options.index_type(),
                        index.r#type
                    ),
                    ptr![
                        "relationships",
                        relationship_id,
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

relationship_walk!(RelationshipIndexOptions);

pub(crate) struct RelationshipIndexProperties;

impl RelationshipValidation for RelationshipIndexProperties {
    fn validate_index(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        _relationship: &Relationship,
        index_id: &str,
        index: &RelationshipIndex,
        issues: &mut Vec<Issue>,
    ) {
        if index.properties.is_empty() {
            issues.push(Issue::new(
                "missing_relationship_index_properties",
                format!(
                    "Index '{index_id}' on relationship '{relationship_id}' must have at least one \
                     property"
                ),
                ptr![
                    "relationships",
                    relationship_id,
                    "extensions",
                    "indexes",
                    index_id,
                    "properties"
                ],
            ));
        }
    }
}

relationship_walk!(RelationshipIndexProperties);

pub(crate) struct RelationshipIndexPropertyDuplicate;

impl RelationshipValidation for RelationshipIndexPropertyDuplicate {
    fn validate_index(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        _relationship: &Relationship,
        index_id: &str,
        index: &RelationshipIndex,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(duplicate_entry_issues(
            "duplicate_relationship_index_property",
            "property",
            &format!("index '{index_id}' on relationship '{relationship_id}'"),
            &index.properties,
            &ptr![
                "relationships",
                relationship_id,
                "extensions",
                "indexes",
                index_id,
                "properties"
            ],
        ));
    }
}

relationship_walk!(RelationshipIndexPropertyDuplicate);
