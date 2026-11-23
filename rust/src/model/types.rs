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

//! Types of the things a model can constrain or index: [ConstraintType] and [IndexType].

use serde::{Deserialize, Serialize};

/// Declares an enum whose variants are fixed document strings, from one table: the enum, its
/// serde forms, `as_str` and its default are generated together, so the listings cannot drift
/// apart. The `#[default]` attribute on a variant also derives `Default` for the enum.
macro_rules! string_enum {
    (
        $(#[$meta:meta])*
        $vis:vis enum $name:ident {
            $( $(#[$variant_meta:meta])* $variant:ident => $string:literal ),* $(,)?
        }
    ) => {
        $(#[$meta])*
        #[derive(Copy, Clone, Debug, Default, PartialEq, Eq, Hash)]
        $vis enum $name {
            $( $(#[$variant_meta])* $variant, )*
        }

        impl $name {
            /// The name of the value as written in a document.
            pub fn as_str(&self) -> &'static str {
                match self {
                    $( $name::$variant => $string, )*
                }
            }
        }

        impl std::fmt::Display for $name {
            fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
                f.write_str(self.as_str())
            }
        }

        impl Serialize for $name {
            fn serialize<S: serde::Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
                serializer.serialize_str(self.as_str())
            }
        }

        impl<'de> Deserialize<'de> for $name {
            fn deserialize<D: serde::Deserializer<'de>>(
                deserializer: D,
            ) -> Result<Self, D::Error> {
                // An owned String: a `&str` borrow fails on values with JSON escapes
                // ("STR\u{49}NG") and on deserialization from an owned `serde_json::Value`.
                let value = <String>::deserialize(deserializer)?;
                match value.as_str() {
                    $( $string => Ok($name::$variant), )*
                    _ => Err(serde::de::Error::unknown_variant(&value, &[$($string),*])),
                }
            }
        }
    };
}

pub(crate) use string_enum;

string_enum! {
    /// The type of a constraint.
    pub enum ConstraintType {
        /// A constraint requiring the property to exist.
        #[default]
        Exists => "EXISTS",
        /// A constraint making the properties a node key.
        Key => "KEY",
        /// A constraint fixing the property's type.
        PropertyType => "PROPERTY_TYPE",
        /// A constraint making the properties unique.
        Unique => "UNIQUE",
    }
}

impl ConstraintType {
    /// The lowercase name used in shorthand constraint ids, see [`crate::model::notation`].
    pub(crate) fn lowercase(&self) -> String {
        self.as_str().to_ascii_lowercase()
    }
}

string_enum! {
    /// The type of an index.
    pub enum IndexType {
        /// A full-text index.
        #[default]
        Fulltext => "FULLTEXT",
        /// A point index.
        Point => "POINT",
        /// A range index.
        Range => "RANGE",
        /// A text index.
        Text => "TEXT",
        /// A vector index.
        Vector => "VECTOR",
        /// A lookup index.
        Lookup => "LOOKUP",
    }
}
