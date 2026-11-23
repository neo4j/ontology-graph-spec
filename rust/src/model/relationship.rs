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

//! A relationship of the graph: its type, its two ends, and the rest of its definition.

use indexmap::IndexMap;
use serde::{Deserialize, Serialize};

use super::constraint::Constraint;
use super::custom::CustomValue;
use super::index_option::IndexOption;
use super::property::Property;
use super::types::IndexType;
use crate::model::is_default;

/// A relationship of the graph.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct Relationship {
    /// Type of the relationship.
    pub r#type: String,
    /// The node the relationship starts at.
    pub from: RelationshipTarget,
    /// The node the relationship ends at.
    pub to: RelationshipTarget,
    /// Properties of the relationship keyed by id.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub properties: IndexMap<String, Property>,
    /// Constraints on the relationship keyed by id.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub constraints: IndexMap<String, RelationshipConstraint>,
    /// Extensions for the relationship.
    #[serde(default, skip_serializing_if = "is_default")]
    pub extensions: RelationshipExtensions,
    /// Description of the relationship.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub description: String,
    /// Alternative names for the relationship.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub aliases: Vec<String>,
    /// Reference or URI of an external resource describing the relationship.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub reference: String,
}

/// One end of a relationship.
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct RelationshipTarget {
    /// Id of the node at this end.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub node: String,
    /// Label of the node at this end.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub label: String,
    /// Exact number of relationships per node at this end. Null when unconstrained.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub count: Option<i64>,
    /// Minimum number of relationships per node at this end.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub min_count: Option<i64>,
    /// Maximum number of relationships per node at this end.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub max_count: Option<i64>,
}

/// A constraint on a relationship: the shared [`Constraint`] shape under the name the format
/// gives it.
pub type RelationshipConstraint = Constraint;

/// Extensions for a relationship.
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct RelationshipExtensions {
    /// Indexes on this relationship keyed by id.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub indexes: IndexMap<String, RelationshipIndex>,
    /// User-defined arbitrary data.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub custom: IndexMap<String, CustomValue>,
}

/// An index on a relationship.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct RelationshipIndex {
    /// Type of the index.
    pub r#type: IndexType,
    /// Ids of the properties the index covers.
    pub properties: Vec<String>,
    /// Configuration specific to the type of index.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub options: Option<IndexOption>,
    /// User-defined arbitrary data.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub custom: IndexMap<String, CustomValue>,
    /// Name of the index in the database.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
}
