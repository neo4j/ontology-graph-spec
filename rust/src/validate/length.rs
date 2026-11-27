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

//! Restricts the length of names and descriptions throughout the model.

use crate::model::GraphModel;

use super::{ptr, Issue, Validation};

pub(crate) struct StringLength;

impl StringLength {
    pub(crate) const MAX_NAME_LENGTH: usize = 256;
    pub(crate) const MAX_CONSTRAINT_INDEX_NAME_LENGTH: usize = 800;
    pub(crate) const MAX_MODEL_DESCRIPTION_LENGTH: usize = 64 * 1024;
    pub(crate) const MAX_DESCRIPTION_LENGTH: usize = 1024;
}

impl Validation for StringLength {
    fn name(&self) -> &'static str {
        "StringLength"
    }

    fn validate(&self, model: &GraphModel, issues: &mut Vec<Issue>) {
        check(
            "name",
            Some(&model.name),
            Self::MAX_NAME_LENGTH,
            issues,
            &ptr!["name"],
        );
        check(
            "description",
            Some(&model.description),
            Self::MAX_MODEL_DESCRIPTION_LENGTH,
            issues,
            &ptr!["description"],
        );

        for (node_id, node) in &model.nodes {
            check_entity(
                &node.description,
                &node.properties,
                &node.constraints,
                &node.extensions.indexes,
                |index: &crate::model::node::NodeIndex| index.name.as_deref(),
                issues,
                ["nodes", node_id],
            );
        }

        for (relationship_id, relationship) in &model.relationships {
            check_entity(
                &relationship.description,
                &relationship.properties,
                &relationship.constraints,
                &relationship.extensions.indexes,
                |index: &crate::model::relationship::RelationshipIndex| index.name.as_deref(),
                issues,
                ["relationships", relationship_id],
            );
        }
    }
}

fn check_entity<I>(
    description: &str,
    properties: &indexmap::IndexMap<String, crate::model::property::Property>,
    constraints: &indexmap::IndexMap<String, crate::model::constraint::Constraint>,
    indexes: &indexmap::IndexMap<String, I>,
    index_name: impl Fn(&I) -> Option<&str>,
    issues: &mut Vec<Issue>,
    entity_path: [&str; 2],
) {
    check(
        "description",
        Some(description),
        StringLength::MAX_DESCRIPTION_LENGTH,
        issues,
        &ptr![entity_path[0], entity_path[1], "description"],
    );
    for (property_id, property) in properties {
        check(
            "name",
            property.name.as_deref(),
            StringLength::MAX_NAME_LENGTH,
            issues,
            &ptr![
                entity_path[0],
                entity_path[1],
                "properties",
                property_id,
                "name"
            ],
        );
        check(
            "description",
            Some(&property.description),
            StringLength::MAX_DESCRIPTION_LENGTH,
            issues,
            &ptr![
                entity_path[0],
                entity_path[1],
                "properties",
                property_id,
                "description"
            ],
        );
    }
    for (constraint_id, constraint) in constraints {
        check(
            "name",
            constraint.name.as_deref(),
            StringLength::MAX_CONSTRAINT_INDEX_NAME_LENGTH,
            issues,
            &ptr![
                entity_path[0],
                entity_path[1],
                "constraints",
                constraint_id,
                "name"
            ],
        );
    }
    for (index_id, index) in indexes {
        check(
            "name",
            index_name(index),
            StringLength::MAX_CONSTRAINT_INDEX_NAME_LENGTH,
            issues,
            &ptr![
                entity_path[0],
                entity_path[1],
                "extensions",
                "indexes",
                index_id,
                "name"
            ],
        );
    }
}

fn check(field: &str, value: Option<&str>, max: usize, issues: &mut Vec<Issue>, path: &str) {
    if let Some(value) = value {
        // The reference counts UTF-16 code units (JVM String.length), so a supplementary-plane
        // character counts as two: count the same way so limits and the reported length agree.
        let length = value.encode_utf16().count();
        if length > max {
            issues.push(Issue::with_details(
                &format!("{field}_too_long"),
                format!(
                    "The {field} at '{path}' is {length} characters long, the maximum is {max}"
                ),
                path.to_owned(),
                [("length", length.to_string()), ("max", max.to_string())],
            ));
        }
    }
}
