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

//! The validators that walk relationships: types, ends, aliases, properties, constraints,
//! indexes and relationship mappings.

use crate::model::property::Property;
use crate::model::relationship::{
    Relationship, RelationshipConstraint, RelationshipIndex, RelationshipTarget,
};
use crate::model::GraphModel;

use super::constraint::missing_property_issues;
use super::duplicate::duplicate_entry_issues;
use super::property::{duplicate_property_name, empty_property_name, pattern_one_of_conflict};
use super::{ptr, Issue, Validation};

/// The parts of a relationship a validator can check, mirroring the reference implementation's
/// relationship-validation interface: validators override what they need, and the shared walk
/// visits every relationship, its properties, its constraints and its indexes in that order.
pub(crate) trait RelationshipValidation: Validation {
    fn validate_relationship(
        &self,
        _model: &GraphModel,
        _relationship_id: &str,
        _relationship: &Relationship,
        _issues: &mut Vec<Issue>,
    ) {
    }

    fn validate_property(
        &self,
        _model: &GraphModel,
        _relationship_id: &str,
        _relationship: &Relationship,
        _property_id: &str,
        _property: &Property,
        _issues: &mut Vec<Issue>,
    ) {
    }

    fn validate_constraint(
        &self,
        _model: &GraphModel,
        _relationship_id: &str,
        _relationship: &Relationship,
        _constraint_id: &str,
        _constraint: &RelationshipConstraint,
        _issues: &mut Vec<Issue>,
    ) {
    }

    fn validate_index(
        &self,
        _model: &GraphModel,
        _relationship_id: &str,
        _relationship: &Relationship,
        _index_id: &str,
        _index: &RelationshipIndex,
        _issues: &mut Vec<Issue>,
    ) {
    }
}

/// Implements the [Validation] walk for a [RelationshipValidation].
macro_rules! relationship_walk {
    ($validator:ty) => {
        relationship_walk!($validator, &[]);
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
                for (relationship_id, relationship) in &model.relationships {
                    self.validate_relationship(model, relationship_id, relationship, issues);
                    for (property_id, property) in &relationship.properties {
                        self.validate_property(
                            model,
                            relationship_id,
                            relationship,
                            property_id,
                            property,
                            issues,
                        );
                    }
                    for (constraint_id, constraint) in &relationship.constraints {
                        self.validate_constraint(
                            model,
                            relationship_id,
                            relationship,
                            constraint_id,
                            constraint,
                            issues,
                        );
                    }
                    for (index_id, index) in &relationship.extensions.indexes {
                        self.validate_index(
                            model,
                            relationship_id,
                            relationship,
                            index_id,
                            index,
                            issues,
                        );
                    }
                }
            }
        }
    };
}

pub(crate) use relationship_walk;
pub(crate) struct RelationshipType;

impl RelationshipValidation for RelationshipType {
    fn validate_relationship(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        relationship: &Relationship,
        issues: &mut Vec<Issue>,
    ) {
        if relationship.r#type.trim().is_empty() {
            issues.push(Issue::new(
                "missing_relationship_type",
                format!("Missing type for relationship '{relationship_id}'"),
                ptr!["relationships", relationship_id, "type"],
            ));
        }
    }
}

relationship_walk!(RelationshipType);

pub(crate) struct RelationshipTypeToken;

impl RelationshipValidation for RelationshipTypeToken {
    fn validate_relationship(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        relationship: &Relationship,
        issues: &mut Vec<Issue>,
    ) {
        let relationship_type = &relationship.r#type;
        if relationship_type.contains(':') || relationship_type.contains('=') {
            issues.push(Issue::new(
                "invalid_relationship_type_token",
                format!(
                    "Relationship type '{relationship_type}' contains invalid characters (: or =) \
                     for bulk import"
                ),
                ptr!["relationships", relationship_id, "type"],
            ));
        }
    }
}

relationship_walk!(RelationshipTypeToken);

pub(crate) struct RelationshipNodes;

impl RelationshipValidation for RelationshipNodes {
    fn validate_relationship(
        &self,
        model: &GraphModel,
        relationship_id: &str,
        relationship: &Relationship,
        issues: &mut Vec<Issue>,
    ) {
        check_end(model, relationship_id, "from", &relationship.from, issues);
        check_end(model, relationship_id, "to", &relationship.to, issues);
    }
}

relationship_walk!(RelationshipNodes);

pub(crate) struct RelationshipTargetCount;

impl RelationshipValidation for RelationshipTargetCount {
    fn validate_relationship(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        relationship: &Relationship,
        issues: &mut Vec<Issue>,
    ) {
        validate_target(relationship_id, "from", &relationship.from, issues);
        validate_target(relationship_id, "to", &relationship.to, issues);
    }
}

relationship_walk!(RelationshipTargetCount);

/// A node id at one end of a relationship must name a node in the model, when set.
fn check_end(
    model: &GraphModel,
    relationship_id: &str,
    side: &str,
    target: &RelationshipTarget,
    issues: &mut Vec<Issue>,
) {
    let node = target.node.as_str();
    if !node.trim().is_empty() && !model.nodes.contains_key(node) {
        issues.push(Issue::new(
            &format!("missing_relationship_{side}_node"),
            format!("Missing node with id '{node}' for relationship '{relationship_id}'"),
            ptr!["relationships", relationship_id, side, "node"],
        ));
    }
}

/// A relationship target may define an exact `count` or a `min_count`/`max_count` range, but not
/// both, and the counts must make sense.
fn validate_target(
    relationship_id: &str,
    side: &str,
    target: &RelationshipTarget,
    issues: &mut Vec<Issue>,
) {
    let path = ptr!["relationships", relationship_id, side];
    let count = target.count;
    let min_count = target.min_count;
    let max_count = target.max_count;
    let has_count = count.is_some();
    let has_range = min_count.is_some() || max_count.is_some();
    if has_count && has_range {
        issues.push(Issue::new(
            "relationship_count_conflict",
            format!(
                "Relationship '{relationship_id}' {side} can't set both count and min/max count"
            ),
            path.clone(),
        ));
    }
    for (value, field, label) in [
        (count, "count", "count"),
        (min_count, "min_count", "min count"),
        (max_count, "max_count", "max count"),
    ] {
        if value.is_some_and(|value| value < 0) {
            issues.push(Issue::new(
                &format!("relationship_{field}_negative"),
                format!("Relationship '{relationship_id}' {side} {label} must not be negative"),
                format!("{path}/{field}"),
            ));
        }
    }
    if let (Some(min_count), Some(max_count)) = (min_count, max_count) {
        if min_count > max_count {
            issues.push(Issue::new(
                "relationship_min_count_exceeds_max",
                format!(
                    "Relationship '{relationship_id}' {side} min count is greater than max count"
                ),
                path.clone(),
            ));
        }
    }
}

pub(crate) struct RelationshipTargetNodes;

impl RelationshipValidation for RelationshipTargetNodes {
    fn validate_relationship(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        relationship: &Relationship,
        issues: &mut Vec<Issue>,
    ) {
        validate_node(relationship_id, "from", &relationship.from, issues);
        validate_node(relationship_id, "to", &relationship.to, issues);
    }
}

relationship_walk!(RelationshipTargetNodes);

/// Every relationship must set the node it starts from and the node it points to.
fn validate_node(
    relationship_id: &str,
    side: &str,
    target: &RelationshipTarget,
    issues: &mut Vec<Issue>,
) {
    if target.node.trim().is_empty() {
        issues.push(Issue::new(
            // The code names the side: missing_relationship_from / missing_relationship_to.
            &format!("missing_relationship_{side}"),
            format!("Missing {side} node for relationship '{relationship_id}'"),
            ptr!["relationships", relationship_id, side, "node"],
        ));
    }
}

pub(crate) struct RelationshipAliasDuplicate;

impl RelationshipValidation for RelationshipAliasDuplicate {
    fn validate_relationship(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        relationship: &Relationship,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(duplicate_entry_issues(
            "duplicate_relationship_alias",
            "alias",
            &format!("relationship '{relationship_id}'"),
            &relationship.aliases,
            &ptr!["relationships", relationship_id, "aliases"],
        ));
    }
}

relationship_walk!(RelationshipAliasDuplicate);

pub(crate) struct RelationshipPropertyAliasDuplicate;

impl RelationshipValidation for RelationshipPropertyAliasDuplicate {
    fn validate_property(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        _relationship: &Relationship,
        property_id: &str,
        property: &Property,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(duplicate_entry_issues(
            "duplicate_relationship_property_alias",
            "alias",
            &format!("property '{property_id}' of relationship '{relationship_id}'"),
            &property.aliases,
            &ptr![
                "relationships",
                relationship_id,
                "properties",
                property_id,
                "aliases"
            ],
        ));
    }
}

relationship_walk!(RelationshipPropertyAliasDuplicate);

pub(crate) struct RelationshipPropertyDuplicateName;

impl RelationshipValidation for RelationshipPropertyDuplicateName {
    fn validate_property(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        relationship: &Relationship,
        property_id: &str,
        property: &Property,
        issues: &mut Vec<Issue>,
    ) {
        if let Some(issue) = duplicate_property_name(
            &relationship.properties,
            property_id,
            property,
            ["relationships", relationship_id],
        ) {
            issues.push(issue);
        }
    }
}

relationship_walk!(RelationshipPropertyDuplicateName);

pub(crate) struct RelationshipPropertyEmptyName;

impl RelationshipValidation for RelationshipPropertyEmptyName {
    fn validate_property(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        _relationship: &Relationship,
        property_id: &str,
        property: &Property,
        issues: &mut Vec<Issue>,
    ) {
        if let Some(issue) =
            empty_property_name(property_id, property, ["relationships", relationship_id])
        {
            issues.push(issue);
        }
    }
}

relationship_walk!(RelationshipPropertyEmptyName);

pub(crate) struct RelationshipPropertyPatternOneOf;

impl RelationshipValidation for RelationshipPropertyPatternOneOf {
    fn validate_property(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        _relationship: &Relationship,
        property_id: &str,
        property: &Property,
        issues: &mut Vec<Issue>,
    ) {
        if let Some(issue) =
            pattern_one_of_conflict(property_id, property, ["relationships", relationship_id])
        {
            issues.push(issue);
        }
    }
}

relationship_walk!(RelationshipPropertyPatternOneOf);

pub(crate) struct RelationshipConstraints;

impl RelationshipValidation for RelationshipConstraints {
    fn validate_constraint(
        &self,
        _model: &GraphModel,
        relationship_id: &str,
        relationship: &Relationship,
        constraint_id: &str,
        constraint: &RelationshipConstraint,
        issues: &mut Vec<Issue>,
    ) {
        issues.extend(missing_property_issues(
            &relationship.properties,
            &constraint.properties,
            "missing_relationship_constraint_property",
            |property| {
                format!(
                    "Missing property with id '{property}' for relationship constraint \
                     '{constraint_id}'"
                )
            },
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

relationship_walk!(RelationshipConstraints);
