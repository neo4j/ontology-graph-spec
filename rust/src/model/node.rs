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

//! A node of the graph: its labels, properties, constraints, indexes and extensions.

use indexmap::IndexMap;
use serde::{Deserialize, Serialize};

use super::constraint::Constraint;
use super::custom::{CustomExtensions, CustomValue};
use super::index_option::IndexOption;
use super::property::Property;
use super::types::IndexType;
use crate::model::is_default;

/// A node of the graph.
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct Node {
    /// Shorthand for the identifying label. Expanded into labels when the model is decoded.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub label: Option<String>,
    /// Labels of the node.
    #[serde(default, skip_serializing_if = "is_default")]
    pub labels: Labels,
    /// Properties of the node keyed by id.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub properties: IndexMap<String, Property>,
    /// Constraints on the node keyed by id.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub constraints: IndexMap<String, NodeConstraint>,
    /// Extensions for the node.
    #[serde(default, skip_serializing_if = "is_default")]
    pub extensions: NodeExtensions,
    /// Description of the node.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub description: String,
    /// Alternative names for the node.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub aliases: Vec<String>,
    /// Reference or URI of an external resource describing the node.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub reference: String,
}

/// The labels of a node.
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct Labels {
    /// The label which identifies the node.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub identifier: Option<String>,
    /// Labels implied by the node in addition to the identifier.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub implied: Vec<String>,
    /// Labels which may be present on the node but aren't required.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub optional: Vec<String>,
    /// Extensions for the labels.
    #[serde(default, skip_serializing_if = "is_default")]
    pub extensions: CustomExtensions,
}

/// A constraint on a node: the shared [`Constraint`] shape under the name the format gives it.
pub type NodeConstraint = Constraint;

/// Extensions for a node.
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct NodeExtensions {
    /// Database indexes on this node keyed by id.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub indexes: IndexMap<String, NodeIndex>,
    /// Optional visual information of this node.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub display: Option<NodeDisplay>,
    /// User-defined arbitrary data.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub custom: IndexMap<String, CustomValue>,
}

/// Visual information for a node.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct NodeDisplay {
    /// Horizontal position.
    pub x: f64,
    /// Vertical position.
    pub y: f64,
    /// Display colour of the node as a hex string, e.g. #FF8800.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub colour: Option<String>,
    /// User-defined arbitrary data.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub custom: IndexMap<String, CustomValue>,
}

/// An index on a node.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct NodeIndex {
    /// Type of the index.
    pub r#type: IndexType,
    /// Labels the index covers.
    pub labels: Vec<String>,
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
