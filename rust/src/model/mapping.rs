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

//! Mappings from data source tables to the graph, used for importing.

use indexmap::IndexMap;
use serde::{Deserialize, Serialize};

use crate::model::is_default;

/// A mapping from a data source table to the graph. Either a node, relationship or query mapping.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(tag = "type", rename_all = "UPPERCASE")]
pub enum Mapping {
    #[serde(rename = "NODE")]
    Node(NodeMapping),
    #[serde(rename = "RELATIONSHIP")]
    Relationship(RelationshipMapping),
    #[serde(rename = "QUERY")]
    Query(QueryMapping),
}

impl Mapping {
    /// Optional id of this mapping, so other mappings in the same list can reference it in
    /// `depends_on`.
    #[must_use]
    pub fn id(&self) -> &str {
        match self {
            Mapping::Node(mapping) => &mapping.id,
            Mapping::Relationship(mapping) => &mapping.id,
            Mapping::Query(mapping) => &mapping.id,
        }
    }

    /// Id of another mapping in the same list which must be imported before this one, or empty
    /// when there is no dependency.
    #[must_use]
    pub fn depends_on(&self) -> &str {
        match self {
            Mapping::Node(mapping) => &mapping.depends_on,
            Mapping::Relationship(mapping) => &mapping.depends_on,
            Mapping::Query(mapping) => &mapping.depends_on,
        }
    }
}

use super::types::string_enum;

string_enum! {
    /// Write approach used when importing data (merged into existing data or always created).
    pub enum MappingMode {
        /// Merge with existing data.
        #[default]
        Merge => "MERGE",
        /// Always create new data.
        Create => "CREATE",
    }
}

/// Maps rows of a table to nodes.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct NodeMapping {
    /// Id of the node being mapped.
    pub node: String,
    /// Id of the source table.
    pub table: String,
    /// Mappings from property ids to source columns.
    pub properties: IndexMap<String, PropertyMapping>,
    /// Whether nodes are merged with existing nodes or always created.
    #[serde(default, skip_serializing_if = "is_default")]
    pub mode: MappingMode,
    /// Label used to match existing nodes.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub match_label: Option<String>,
    /// Ids of the properties which identify a node.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub key: Vec<String>,
    /// Id of another mapping in the same list which must be imported before this one. Empty when
    /// there is no dependency.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub depends_on: String,
    /// Optional id of this mapping, so other mappings in the same list can reference it in
    /// `depends_on`.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub id: String,
}

/// Maps rows of a table to relationships.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct RelationshipMapping {
    /// Id of the relationship being mapped.
    pub relationship: String,
    /// Id of the source table.
    pub table: String,
    /// How the start node of the relationship is matched.
    pub from_node: TargetMapping,
    /// How the end node of the relationship is matched.
    pub to_node: TargetMapping,
    /// Label used to match existing relationships.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub match_label: Option<String>,
    /// Mappings from property ids to source columns.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub properties: IndexMap<String, PropertyMapping>,
    /// Whether relationships are merged with existing relationships or always created.
    #[serde(default, skip_serializing_if = "is_default")]
    pub mode: MappingMode,
    /// Ids of the properties which identify a relationship.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub key: Vec<String>,
    /// Id of another mapping in the same list which must be imported before this one. Empty when
    /// there is no dependency.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub depends_on: String,
    /// Optional id of this mapping, so other mappings in the same list can reference it in
    /// `depends_on`.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub id: String,
}

/// Maps the results of a query run against a table.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct QueryMapping {
    /// Id of the source table.
    pub table: String,
    /// The query to run.
    pub query: String,
    /// Id of another mapping in the same list which must be imported before this one. Empty when
    /// there is no dependency.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub depends_on: String,
    /// Optional id of this mapping, so other mappings in the same list can reference it in
    /// `depends_on`.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub id: String,
}

/// Maps a property to a source column.
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct PropertyMapping {
    /// Name of the source column.
    pub column: String,
}

/// Identifies a node at one end of a mapped relationship.
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct TargetMapping {
    /// Label of the node.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub label: String,
    /// Mappings from property ids to source columns used to match the node.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub properties: IndexMap<String, PropertyMapping>,
}
