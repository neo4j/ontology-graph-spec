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

//! Arbitrary user data stored in `custom` maps throughout the model.
//!
//! Plain values only: booleans, integers, doubles, strings, lists and maps, with string keys.
//! Nulls are rejected, as in the reference implementation, and nesting is limited to
//! [MAX_CUSTOM_DEPTH] levels.

use indexmap::IndexMap;
use serde::de::{self, MapAccess, SeqAccess, Visitor};
use serde::ser::{SerializeMap, SerializeSeq};
use serde::{Deserialize, Deserializer, Serialize, Serializer};

/// Maximum nesting in custom values to guard against stack overflows.
pub const MAX_CUSTOM_DEPTH: usize = 128;

/// A value in a `custom` map.
#[derive(Clone, Debug, PartialEq)]
pub enum CustomValue {
    Boolean(bool),
    Integer(i64),
    Float(f64),
    String(String),
    List(Vec<CustomValue>),
    Map(CustomMap),
}

/// An insertion-ordered map of custom values, so documents round-trip unchanged.
pub type CustomMap = IndexMap<String, CustomValue>;

/// Extensions that carry only user-defined custom data: the shape of every element whose
/// extensions hold nothing else.
#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct CustomExtensions {
    /// User-defined arbitrary data.
    #[serde(default, skip_serializing_if = "IndexMap::is_empty")]
    pub custom: CustomMap,
}

const NOT_NULL: &str = "Custom values cannot be null";

impl Serialize for CustomValue {
    fn serialize<S: Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
        match self {
            CustomValue::Boolean(value) => serializer.serialize_bool(*value),
            CustomValue::Integer(value) => serializer.serialize_i64(*value),
            CustomValue::Float(value) => {
                // JSON has no non-finite float: serde_json would silently write null, which
                // custom-value decoding rejects. Fail with a useful error instead.
                if !value.is_finite() {
                    return Err(serde::ser::Error::custom(format!(
                        "non-finite float {value} cannot be encoded: JSON and YAML have no representation for it"
                    )));
                }
                serializer.serialize_f64(*value)
            }
            CustomValue::String(value) => serializer.serialize_str(value),
            CustomValue::List(values) => {
                let mut seq = serializer.serialize_seq(Some(values.len()))?;
                for value in values {
                    seq.serialize_element(value)?;
                }
                seq.end()
            }
            CustomValue::Map(values) => {
                let mut map = serializer.serialize_map(Some(values.len()))?;
                for (key, value) in values {
                    map.serialize_entry(key, value)?;
                }
                map.end()
            }
        }
    }
}

impl<'de> Deserialize<'de> for CustomValue {
    fn deserialize<D: Deserializer<'de>>(deserializer: D) -> Result<Self, D::Error> {
        struct CustomValueVisitor;

        impl<'de> Visitor<'de> for CustomValueVisitor {
            type Value = CustomValue;

            fn expecting(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
                formatter.write_str("a custom value: boolean, number, string, list or map")
            }

            fn visit_bool<E: de::Error>(self, value: bool) -> Result<Self::Value, E> {
                Ok(CustomValue::Boolean(value))
            }

            fn visit_i64<E: de::Error>(self, value: i64) -> Result<Self::Value, E> {
                Ok(CustomValue::Integer(value))
            }

            fn visit_u64<E: de::Error>(self, value: u64) -> Result<Self::Value, E> {
                // Numbers beyond i64 fall back to doubles, as the reference implementation does
                // when a value fits neither a long nor is a string.
                if value <= i64::MAX as u64 {
                    Ok(CustomValue::Integer(value as i64))
                } else {
                    Ok(CustomValue::Float(value as f64))
                }
            }

            fn visit_f64<E: de::Error>(self, value: f64) -> Result<Self::Value, E> {
                Ok(CustomValue::Float(value))
            }

            fn visit_str<E: de::Error>(self, value: &str) -> Result<Self::Value, E> {
                Ok(CustomValue::String(value.to_owned()))
            }

            fn visit_string<E: de::Error>(self, value: String) -> Result<Self::Value, E> {
                Ok(CustomValue::String(value))
            }

            fn visit_none<E: de::Error>(self) -> Result<Self::Value, E> {
                Err(E::custom(NOT_NULL))
            }

            fn visit_unit<E: de::Error>(self) -> Result<Self::Value, E> {
                Err(E::custom(NOT_NULL))
            }

            fn visit_seq<A: SeqAccess<'de>>(self, mut seq: A) -> Result<Self::Value, A::Error> {
                let _guard = DepthGuard::enter()?;
                let mut values = Vec::new();
                while let Some(value) = seq.next_element::<CustomValue>()? {
                    values.push(value);
                }
                Ok(CustomValue::List(values))
            }

            fn visit_map<A: MapAccess<'de>>(self, mut map: A) -> Result<Self::Value, A::Error> {
                let _guard = DepthGuard::enter()?;
                let mut values = CustomMap::new();
                while let Some((key, value)) = map.next_entry::<String, CustomValue>()? {
                    values.insert(key, value);
                }
                Ok(CustomValue::Map(values))
            }
        }

        deserializer.deserialize_any(CustomValueVisitor)
    }
}

// The current nesting depth of lists and maps being read, shared by all [CustomValue] reads on
// the thread. The deserializer walks depth-first, so a value cannot know its own depth.
thread_local! {
    static DEPTH: std::cell::Cell<usize> = const { std::cell::Cell::new(0) };
}

/// Enters one more level of nesting, failing if that would exceed [MAX_CUSTOM_DEPTH], and leaves
/// it again when dropped.
struct DepthGuard;

impl DepthGuard {
    fn enter<E: serde::de::Error>() -> Result<DepthGuard, E> {
        DEPTH.with(|cell| {
            let depth = cell.get() + 1;
            if depth > MAX_CUSTOM_DEPTH {
                return Err(E::custom(format!(
                    "Custom values cannot be nested more than {MAX_CUSTOM_DEPTH} levels deep"
                )));
            }
            cell.set(depth);
            Ok(DepthGuard)
        })
    }
}

impl Drop for DepthGuard {
    fn drop(&mut self) {
        DEPTH.with(|cell| cell.set(cell.get().saturating_sub(1)));
    }
}
