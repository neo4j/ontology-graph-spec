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

//! The validators that walk the constraints of relationships.

use crate::model::relationship::{Relationship, RelationshipConstraint};
use crate::model::types::ConstraintType;
use crate::model::GraphModel;

use super::constraint::{
    duplicate_constraint_ids, duplicate_property_set_issue, has_single_distinct_property,
    redundant_key_overlap_issues,
};
use super::duplicate::duplicate_entry_issues;
use super::relationship::{relationship_walk, RelationshipValidation};
use super::{ptr, Issue, Validation};

pub(crate) struct RelationshipConstraintDuplicatePropertySet;

impl RelationshipValidation for RelationshipConstraintDuplicatePropertySet {
    fn validate_constraint(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        relationship: &Relationship,
        constraint_id: &str,
        constraint: &RelationshipConstraint,
        issues: &mut Vec<Issue>,
    ) {
        let duplicates =
            duplicate_constraint_ids(constraint_id, constraint, &relationship.constraints);
        if !duplicates.is_empty() {
            issues.push(duplicate_property_set_issue(
                constraint_id,
                constraint,
                &duplicates,
                relationship_id,
                "relationship",
                "duplicate_relationship_constraint_property_set",
                "relationships",
            ));
        }
    }
}

relationship_walk!(RelationshipConstraintDuplicatePropertySet);

pub(crate) struct RelationshipConstraintPropertyDuplicate;

impl RelationshipValidation for RelationshipConstraintPropertyDuplicate {
    fn validate_constraint(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        _relationship: &Relationship,
        constraint_id: &str,
        constraint: &RelationshipConstraint,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(duplicate_entry_issues(
            "duplicate_relationship_constraint_property",
            "property",
            &format!("constraint '{constraint_id}' on relationship '{relationship_id}'"),
            &constraint.properties,
            &ptr![
                "relationships",
                relationship_id,
                "constraints",
                constraint_id,
                "properties"
            ],
        ));
    }
}

relationship_walk!(RelationshipConstraintPropertyDuplicate);

pub(crate) struct RelationshipExistenceConstraint;

impl RelationshipValidation for RelationshipExistenceConstraint {
    fn validate_constraint(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        _relationship: &Relationship,
        constraint_id: &str,
        constraint: &RelationshipConstraint,
        issues: &mut Vec<Issue>,
    ) {
        if constraint.r#type == ConstraintType::Exists
            && !has_single_distinct_property(&constraint.properties)
        {
            issues.push(Issue::new(
                "invalid_relationship_exist_constraint_property_count",
                format!(
                    "Relationship existence constraint '{constraint_id}' must have exactly one \
                     property."
                ),
                ptr![
                    "relationships",
                    relationship_id,
                    "constraints",
                    constraint_id,
                    "properties"
                ],
            ));
        }
    }
}

relationship_walk!(
    RelationshipExistenceConstraint,
    &["RelationshipConstraints"]
);

pub(crate) struct RelationshipTypeConstraint;

impl RelationshipValidation for RelationshipTypeConstraint {
    fn validate_constraint(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        _relationship: &Relationship,
        constraint_id: &str,
        constraint: &RelationshipConstraint,
        issues: &mut Vec<Issue>,
    ) {
        if constraint.r#type == ConstraintType::PropertyType
            && !has_single_distinct_property(&constraint.properties)
        {
            issues.push(Issue::new(
                "invalid_relationship_type_constraint_property_count",
                format!(
                    "Relationship type constraint '{constraint_id}' must have exactly one property."
                ),
                ptr![
                    "relationships",
                    relationship_id,
                    "constraints",
                    constraint_id,
                    "properties"
                ],
            ));
        }
    }
}

relationship_walk!(RelationshipTypeConstraint, &["RelationshipConstraints"]);

pub(crate) struct RelationshipKeyOverlap;

impl RelationshipValidation for RelationshipKeyOverlap {
    fn validate_relationship(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        relationship: &Relationship,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(redundant_key_overlap_issues(
            relationship_id,
            &relationship.constraints,
            "relationships",
            "relationship",
            "redundant_relationship_constraint_key_overlap",
        ));
    }
}

relationship_walk!(RelationshipKeyOverlap);
