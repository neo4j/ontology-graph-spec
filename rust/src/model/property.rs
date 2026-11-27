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

//! A property of a node or relationship, and the Neo4j types it can have.

use serde::{Deserialize, Serialize};

use super::custom::CustomExtensions;
use crate::model::is_default;

use super::types::string_enum;

string_enum! {
    /// A Neo4j property type.
    pub enum Neo4jType {
        /// Any type, the default when a property doesn't say.
        #[default]
        Any => "ANY",
        /// A list of any type.
        ListAny => "LIST<ANY>",
        /// A boolean.
        Boolean => "BOOLEAN",
        /// A list of booleans.
        ListBoolean => "LIST<BOOLEAN>",
        /// A date.
        Date => "DATE",
        /// A list of dates.
        ListDate => "LIST<DATE>",
        /// A duration.
        Duration => "DURATION",
        /// A list of durations.
        ListDuration => "LIST<DURATION>",
        /// A 32-bit float.
        Float32 => "FLOAT32",
        /// A list of 32-bit floats.
        ListFloat32 => "LIST<FLOAT32>",
        /// A 64-bit float.
        Float => "FLOAT",
        /// A list of 64-bit floats.
        ListFloat => "LIST<FLOAT>",
        /// An 8-bit integer.
        Integer8 => "INTEGER8",
        /// A list of 8-bit integers.
        ListInteger8 => "LIST<INTEGER8>",
        /// A 16-bit integer.
        Integer16 => "INTEGER16",
        /// A list of 16-bit integers.
        ListInteger16 => "LIST<INTEGER16>",
        /// A 32-bit integer.
        Integer32 => "INTEGER32",
        /// A list of 32-bit integers.
        ListInteger32 => "LIST<INTEGER32>",
        /// A 64-bit integer.
        Integer => "INTEGER",
        /// A list of 64-bit integers.
        ListInteger => "LIST<INTEGER>",
        /// A local datetime.
        LocalDatetime => "LOCAL DATETIME",
        /// A list of local datetimes.
        ListLocalDatetime => "LIST<LOCAL DATETIME>",
        /// A local time.
        LocalTime => "LOCAL TIME",
        /// A list of local times.
        ListLocalTime => "LIST<LOCAL TIME>",
        /// A point.
        Point => "POINT",
        /// A list of points.
        ListPoint => "LIST<POINT>",
        /// A string.
        String => "STRING",
        /// A list of strings.
        ListString => "LIST<STRING>",
        /// A vector of 64-bit floats.
        VectorFloat => "VECTOR<FLOAT>",
        /// A vector of 32-bit floats.
        VectorFloat32 => "VECTOR<FLOAT32>",
        /// A vector of 64-bit integers.
        VectorInteger => "VECTOR<INTEGER>",
        /// A vector of 32-bit integers.
        VectorInteger32 => "VECTOR<INTEGER32>",
        /// A vector of 16-bit integers.
        VectorInteger16 => "VECTOR<INTEGER16>",
        /// A vector of 8-bit integers.
        VectorInteger8 => "VECTOR<INTEGER8>",
        /// A zoned datetime.
        ZonedDatetime => "ZONED DATETIME",
        /// A list of zoned datetimes.
        ListZonedDatetime => "LIST<ZONED DATETIME>",
        /// A zoned time.
        ZonedTime => "ZONED TIME",
        /// A list of zoned times.
        ListZonedTime => "LIST<ZONED TIME>",
        /// A UUID.
        Uuid => "UUID",
    }
}

impl Neo4jType {
    /// Whether the type is usable as a bulk-import key or composite constraint property:
    /// the reference's bulk import accepts STRING and INTEGER only.
    #[must_use]
    pub fn is_bulk_importable_key_type(&self) -> bool {
        matches!(self, Neo4jType::String | Neo4jType::Integer)
    }

    /// Whether the type is a vector type.
    #[must_use]
    pub fn is_vector(&self) -> bool {
        matches!(
            self,
            Neo4jType::VectorFloat
                | Neo4jType::VectorFloat32
                | Neo4jType::VectorInteger
                | Neo4jType::VectorInteger32
                | Neo4jType::VectorInteger16
                | Neo4jType::VectorInteger8
        )
    }
}

/// A property of a node or relationship.
///
/// Missing fields default as in the format, and default values are omitted when encoding:
/// a property decoded from `{}` keeps [`Neo4jType::Any`] and writes back as `{}`.
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct Property {
    /// Neo4j type of the property.
    #[serde(default, skip_serializing_if = "is_default")]
    pub r#type: Neo4jType,
    /// Dimension of the property when it holds a vector.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub dimension: Option<i64>,
    /// Shorthand for an existence constraint. Expanded into constraints when the model is decoded.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub must_exist: Option<bool>,
    /// Shorthand for a uniqueness constraint. Expanded into constraints when the model is decoded.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub unique: Option<bool>,
    /// Shorthand for a key constraint. Expanded into constraints when the model is decoded.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub key: Option<bool>,
    /// Extensions for the property.
    #[serde(default, skip_serializing_if = "is_default")]
    pub extensions: CustomExtensions,
    /// Name of the property in the database, when different from its id.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Description of the property.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub description: String,
    /// Alternative names for the property.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub aliases: Vec<String>,
    /// Reference or URI of an external resource describing the property.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub reference: String,
    /// Pattern values of the property are expected to match.
    #[serde(default, skip_serializing_if = "String::is_empty")]
    pub pattern: String,
    /// Values the property is restricted to.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub one_of: Vec<String>,
}
