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

//! The typed model a document decodes into: [`GraphModel`], its nodes, relationships, properties,
//! constraints, indexes, mappings and data source tables.
//!
//! Maps keep the order of the document, so an encoded model lists its entries the way they were
//! written. Fields at their default value are omitted when encoding, as in the reference
//! implementation: a node decoded from `{}` writes back as `{}`.

pub mod constraint;
pub mod custom;
pub mod index_option;
pub mod mapping;
pub mod name_format;
pub mod node;
pub mod notation;
pub mod property;
pub mod relationship;
pub mod source;
pub mod types;
pub mod version;

use indexmap::IndexMap;
use serde::{Deserialize, Serialize};

use crate::validate::ValidationSet;
use custom::CustomValue;
use mapping::Mapping;
use node::Node;
use relationship::Relationship;
use source::Table;

/// A graph model: the whole ontology.
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct GraphModel {
    /// Schema specification uri used.
    pub schema: String,
    /// User-defined version of this model document, e.g. a revision number or label.
    pub version: String,
    /// Unique identifier of the model, typically a kebab-case version of the name, used for
    /// referencing.
    pub id: String,
    /// Human readable name for the model.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub name: String,
    /// Description of the model.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub description: String,
    /// Node definitions keyed by node id.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub nodes: IndexMap<String, Node>,
    /// Relationship definitions keyed by relationship id.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub relationships: IndexMap<String, Relationship>,
    /// Extensions to the model: data source tables, mappings and external custom data.
    #[serde(default, skip_serializing_if = "is_default")]
    pub extensions: GraphModelExtensions,
}

impl GraphModel {
    /// Expands any shorthand in the model into its longhand form, see [`notation`].
    pub fn longhand(&mut self) {
        notation::longhand(self);
    }

    /// Checks the model against a [set of validators](crate::validate), returning every issue found.
    #[must_use]
    pub fn validate(&self, set: &ValidationSet) -> Vec<crate::validate::Issue> {
        crate::validate::validate(self, set)
    }
}

/// Extensions for a [`GraphModel`].
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct GraphModelExtensions {
    /// Data source table schemas keyed by table id.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub data_source_tables: IndexMap<String, Table>,
    /// Sets of data source mappings from tables to nodes and relationships, often used for
    /// importing.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub data_source_mappings: IndexMap<String, Vec<Mapping>>,
    /// User-defined arbitrary data.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub custom: IndexMap<String, CustomValue>,
}

/// Whether a value is at its default, so it can be omitted when encoding.
pub(crate) fn is_default<T: Default + PartialEq>(value: &T) -> bool {
    *value == T::default()
}

/// Whether a boolean field whose default is `true` is at it, so it can be omitted when encoding.
pub(crate) fn is_true(value: &bool) -> bool {
    *value
}

/// The default of a boolean field whose default is `true`, for `#[serde(default = ...)]`.
pub(crate) fn default_true() -> bool {
    true
}
