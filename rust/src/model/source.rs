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

//! Data source table schemas, the tables mappings read from.

use indexmap::IndexMap;
use serde::{Deserialize, Serialize};

use super::custom::CustomValue;
use super::property::Neo4jType;
use crate::model::{default_true, is_default, is_true};

/// A data source table schema.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct Table {
    /// Where the table's data comes from.
    pub source: String,
    /// Columns of the table in their source order.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub columns: Vec<TableColumn>,
    /// Names of the columns making up the primary key.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub primary_keys: Vec<String>,
    /// Foreign keys of the table keyed by id.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub foreign_keys: IndexMap<String, ForeignKey>,
    /// User-defined arbitrary data.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub custom: IndexMap<String, CustomValue>,
}

/// A column of a data source table.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct TableColumn {
    /// Name of the column in the source. Unique within the table.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub name: String,
    /// Data type of the column in the source.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub r#type: String,
    /// Size of the column.
    #[serde(default = "default_size", skip_serializing_if = "is_minus_one")]
    pub size: i64,
    /// Neo4j type suggested for values of this column.
    #[serde(default, skip_serializing_if = "is_default")]
    pub suggested: Neo4jType,
    /// Neo4j types values of this column can be converted to when using the import tool.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub supported: Vec<Neo4jType>,
    /// Whether the column can hold null values.
    #[serde(default = "default_true", skip_serializing_if = "is_true")]
    pub nullable: bool,
    /// Dimension of the column when it holds vectors.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub dimension: Option<i64>,
    /// User-defined arbitrary data.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub custom: IndexMap<String, CustomValue>,
}

impl Default for TableColumn {
    fn default() -> Self {
        TableColumn {
            name: String::new(),
            r#type: String::new(),
            size: default_size(),
            suggested: Neo4jType::Any,
            supported: Vec::new(),
            nullable: default_true(),
            dimension: None,
            custom: IndexMap::new(),
        }
    }
}

fn default_size() -> i64 {
    -1
}

fn is_minus_one(value: &i64) -> bool {
    *value == -1
}

/// A foreign key of a table.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct ForeignKey {
    /// Names of the columns in this table which make up the key.
    pub columns: Vec<String>,
    /// The table and columns the key refers to.
    pub references: ForeignKeyReference,
    /// User-defined arbitrary data.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub custom: IndexMap<String, CustomValue>,
}

/// The target of a foreign key.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct ForeignKeyReference {
    /// Id of the referenced table.
    pub table: String,
    /// Names of the referenced columns.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub columns: Vec<String>,
    /// User-defined arbitrary data.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub custom: IndexMap<String, CustomValue>,
}
