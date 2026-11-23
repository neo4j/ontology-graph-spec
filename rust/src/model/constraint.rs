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

//! A constraint on a node or relationship.

use serde::{Deserialize, Serialize};

use super::custom::CustomExtensions;
use super::types::ConstraintType;
use crate::model::is_default;

/// A constraint on a node or relationship. The format gives both the same shape, so one type
/// serves both, as the [`NodeConstraint`](crate::model::node::NodeConstraint) and
/// [`RelationshipConstraint`](crate::model::relationship::RelationshipConstraint) aliases.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct Constraint {
    /// Type of the constraint.
    pub r#type: ConstraintType,
    /// Ids of the properties the constraint covers.
    pub properties: Vec<String>,
    /// Extensions for the constraint.
    #[serde(default, skip_serializing_if = "is_default")]
    pub extensions: CustomExtensions,
    /// Name of the constraint in the database.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
}
