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

//! Configuration specific to the type of an index.
//!
//! Every field has the same default as the reference implementation, and a field at its default
//! is omitted when encoding: an options object that only sets what differs from the default
//! round-trips to itself.

use serde::{Deserialize, Serialize};

use super::types::IndexType;
use crate::model::{default_true, is_true};

fn is_standard_no_stop_words(value: &String) -> bool {
    *value == default_analyzer()
}

fn is_zero_seconds(value: &String) -> bool {
    *value == zero_seconds()
}

fn is_one(value: &i64) -> bool {
    *value == one()
}

fn is_ten_thousand(value: &i64) -> bool {
    *value == ten_thousand()
}

fn is_cosine(value: &String) -> bool {
    *value == cosine()
}

/// Serializes an `f32` as its shortest round-trip decimal, as the reference's `Float` encoding
/// does. A plain `serialize_f32`/`to_value` widens to `f64` first, writing `1.1` as
/// `1.100000023841858`.
fn serialize_shortest_f32<S: serde::Serializer>(
    value: &Option<f32>,
    serializer: S,
) -> Result<S::Ok, S::Error> {
    match value {
        // Display writes the shortest decimal that round-trips the f32; parsing it as f64 keeps
        // that spelling (f32 shortest decimals have at most 9 digits, so they round-trip f64 too).
        Some(value) if value.is_finite() => {
            serializer.serialize_f64(format!("{value}").parse().unwrap_or(f64::from(*value)))
        }
        // JSON has no non-finite float: fail rather than silently write null.
        Some(value) => Err(serde::ser::Error::custom(format!(
            "non-finite float {value} cannot be encoded: JSON and YAML have no representation for it"
        ))),
        None => serializer.serialize_none(),
    }
}

/// Serializes finite `f64` coordinates; a non-finite one fails rather than encoding as null.
fn serialize_finite_f64s<S: serde::Serializer>(
    values: &Vec<f64>,
    serializer: S,
) -> Result<S::Ok, S::Error> {
    if let Some(value) = values.iter().find(|value| !value.is_finite()) {
        return Err(serde::ser::Error::custom(format!(
            "non-finite float {value} cannot be encoded: JSON and YAML have no representation for it"
        )));
    }
    values.serialize(serializer)
}

fn is_scalar(value: &String) -> bool {
    *value == scalar()
}

fn is_sixteen(value: &i64) -> bool {
    *value == sixteen()
}

fn is_hundred(value: &i64) -> bool {
    *value == hundred()
}

fn one() -> i64 {
    1
}

fn ten_thousand() -> i64 {
    10_000
}

fn cosine() -> String {
    "cosine".to_owned()
}

fn scalar() -> String {
    "scalar".to_owned()
}

fn sixteen() -> i64 {
    16
}

fn hundred() -> i64 {
    100
}

/// Configuration specific to the type of an index.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
#[serde(tag = "type", rename_all = "UPPERCASE")]
pub enum IndexOption {
    #[serde(rename = "FULLTEXT")]
    Fulltext(FulltextIndexOption),
    #[serde(rename = "POINT")]
    Point(PointIndexOption),
    #[serde(rename = "VECTOR")]
    Vector(VectorIndexOption),
}

impl IndexOption {
    /// The index type this option configures.
    #[must_use]
    pub fn index_type(&self) -> IndexType {
        match self {
            IndexOption::Fulltext(_) => IndexType::Fulltext,
            IndexOption::Point(_) => IndexType::Point,
            IndexOption::Vector(_) => IndexType::Vector,
        }
    }
}

/// Configuration of a full-text index.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub struct FulltextIndexOption {
    /// Analyzer used when the index doesn't specify one.
    #[serde(
        rename = "fulltext.default_analyzer",
        default = "default_analyzer",
        skip_serializing_if = "is_standard_no_stop_words"
    )]
    pub default_analyzer: String,
    /// Analyzer used by the index.
    #[serde(
        rename = "fulltext.analyzer",
        default,
        skip_serializing_if = "Option::is_none"
    )]
    pub analyzer: Option<String>,
    /// Whether the index is eventually consistent, updating asynchronously after transactions
    /// commit.
    #[serde(
        rename = "fulltext.eventually_consistent",
        default,
        skip_serializing_if = "std::ops::Not::not"
    )]
    pub eventually_consistent: bool,
    /// Number of threads used to apply updates to an eventually consistent index.
    #[serde(
        rename = "fulltext.eventually_consistent_apply_parallelism",
        default = "one",
        skip_serializing_if = "is_one"
    )]
    pub eventually_consistent_apply_parallelism: i64,
    /// How often an eventually consistent index is refreshed.
    #[serde(
        rename = "fulltext.eventually_consistent_refresh_interval",
        default = "zero_seconds",
        skip_serializing_if = "is_zero_seconds"
    )]
    pub eventually_consistent_refresh_interval: String,
    /// Number of threads used to refresh an eventually consistent index.
    #[serde(
        rename = "fulltext.eventually_consistent_refresh_parallelism",
        default = "one",
        skip_serializing_if = "is_one"
    )]
    pub eventually_consistent_refresh_parallelism: i64,
    /// Maximum number of pending updates queued for an eventually consistent index.
    #[serde(
        rename = "fulltext.eventually_consistent_index_update_queue_max_length",
        default = "ten_thousand",
        skip_serializing_if = "is_ten_thousand"
    )]
    pub eventually_consistent_update_queue_max_length: i64,
}

fn default_analyzer() -> String {
    "standard-no-stop-words".to_owned()
}

fn zero_seconds() -> String {
    "0s".to_owned()
}

/// Configuration of a point index.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub struct PointIndexOption {
    /// Lower bounds of the 2D cartesian space.
    #[serde(
        rename = "spatial.cartesian.min",
        default = "cartesian_2d_min",
        skip_serializing_if = "is_cartesian_2d_min"
    )]
    #[serde(serialize_with = "serialize_finite_f64s")]
    pub cartesian_min: Vec<f64>,
    /// Upper bounds of the 2D cartesian space.
    #[serde(
        rename = "spatial.cartesian.max",
        default = "cartesian_2d_max",
        skip_serializing_if = "is_cartesian_2d_max"
    )]
    #[serde(serialize_with = "serialize_finite_f64s")]
    pub cartesian_max: Vec<f64>,
    /// Lower bounds of the 3D cartesian space.
    #[serde(
        rename = "spatial.cartesian-3d.min",
        default = "cartesian_3d_min",
        skip_serializing_if = "is_cartesian_3d_min"
    )]
    #[serde(serialize_with = "serialize_finite_f64s")]
    pub cartesian_3d_min: Vec<f64>,
    /// Upper bounds of the 3D cartesian space.
    #[serde(
        rename = "spatial.cartesian-3d.max",
        default = "cartesian_3d_max",
        skip_serializing_if = "is_cartesian_3d_max"
    )]
    #[serde(serialize_with = "serialize_finite_f64s")]
    pub cartesian_3d_max: Vec<f64>,
    /// Lower bounds of the 2D WGS-84 space.
    #[serde(
        rename = "spatial.wgs-84.min",
        default = "wgs84_2d_min",
        skip_serializing_if = "is_wgs84_2d_min"
    )]
    #[serde(serialize_with = "serialize_finite_f64s")]
    pub wgs84_min: Vec<f64>,
    /// Upper bounds of the 2D WGS-84 space.
    #[serde(
        rename = "spatial.wgs-84.max",
        default = "wgs84_2d_max",
        skip_serializing_if = "is_wgs84_2d_max"
    )]
    #[serde(serialize_with = "serialize_finite_f64s")]
    pub wgs84_max: Vec<f64>,
    /// Lower bounds of the 3D WGS-84 space.
    #[serde(
        rename = "spatial.wgs-84-3d.min",
        default = "wgs84_3d_min",
        skip_serializing_if = "is_wgs84_3d_min"
    )]
    #[serde(serialize_with = "serialize_finite_f64s")]
    pub wgs84_3d_min: Vec<f64>,
    /// Upper bounds of the 3D WGS-84 space.
    #[serde(
        rename = "spatial.wgs-84-3d.max",
        default = "wgs84_3d_max",
        skip_serializing_if = "is_wgs84_3d_max"
    )]
    #[serde(serialize_with = "serialize_finite_f64s")]
    pub wgs84_3d_max: Vec<f64>,
}

fn cartesian_2d_min() -> Vec<f64> {
    vec![-1000000.0, -1000000.0]
}

fn cartesian_2d_max() -> Vec<f64> {
    vec![1000000.0, 1000000.0]
}

fn cartesian_3d_min() -> Vec<f64> {
    vec![-1000000.0, -1000000.0, -1000000.0]
}

fn cartesian_3d_max() -> Vec<f64> {
    vec![1000000.0, 1000000.0, 1000000.0]
}

fn wgs84_2d_min() -> Vec<f64> {
    vec![-180.0, -90.0]
}

fn wgs84_2d_max() -> Vec<f64> {
    vec![180.0, 90.0]
}

fn wgs84_3d_min() -> Vec<f64> {
    vec![-180.0, -90.0, -1000000.0]
}

fn wgs84_3d_max() -> Vec<f64> {
    vec![180.0, 90.0, 1000000.0]
}

fn is_cartesian_2d_min(value: &Vec<f64>) -> bool {
    *value == cartesian_2d_min()
}

fn is_cartesian_2d_max(value: &Vec<f64>) -> bool {
    *value == cartesian_2d_max()
}

fn is_cartesian_3d_min(value: &Vec<f64>) -> bool {
    *value == cartesian_3d_min()
}

fn is_cartesian_3d_max(value: &Vec<f64>) -> bool {
    *value == cartesian_3d_max()
}

fn is_wgs84_2d_min(value: &Vec<f64>) -> bool {
    *value == wgs84_2d_min()
}

fn is_wgs84_2d_max(value: &Vec<f64>) -> bool {
    *value == wgs84_2d_max()
}

fn is_wgs84_3d_min(value: &Vec<f64>) -> bool {
    *value == wgs84_3d_min()
}

fn is_wgs84_3d_max(value: &Vec<f64>) -> bool {
    *value == wgs84_3d_max()
}

/// Configuration of a vector index.
#[derive(Clone, Debug, PartialEq, Serialize, Deserialize)]
pub struct VectorIndexOption {
    /// Number of dimensions of the indexed vectors.
    #[serde(
        rename = "vector.dimensions",
        default,
        skip_serializing_if = "Option::is_none"
    )]
    pub dimensions: Option<i64>,
    /// Function used to compare vectors, e.g. cosine or euclidean.
    #[serde(
        rename = "vector.similarity_function",
        default = "cosine",
        skip_serializing_if = "is_cosine"
    )]
    pub similarity_function: String,
    /// Default expansion factor applied to searches of the index.
    #[serde(
        rename = "vector.default_search_expansion_factor",
        default,
        skip_serializing_if = "Option::is_none",
        serialize_with = "serialize_shortest_f32"
    )]
    pub default_search_expansion_factor: Option<f32>,
    /// Whether vector quantization is enabled.
    #[serde(
        rename = "vector.quantization.enabled",
        default = "default_true",
        skip_serializing_if = "is_true"
    )]
    pub quantization_enabled: bool,
    /// Type of vector quantization used.
    #[serde(
        rename = "vector.quantization.type",
        default = "scalar",
        skip_serializing_if = "is_scalar"
    )]
    pub quantization_type: String,
    /// Maximum number of connections per node in the HNSW graph.
    #[serde(
        rename = "vector.hnsw.m",
        default = "sixteen",
        skip_serializing_if = "is_sixteen"
    )]
    pub hnsw_m: i64,
    /// Number of nearest neighbours considered when building the HNSW graph.
    #[serde(
        rename = "vector.hnsw.ef_construction",
        default = "hundred",
        skip_serializing_if = "is_hundred"
    )]
    pub hnsw_ef_construction: i64,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn options_round_trip_through_their_defaults() {
        // Only what differs from the default is written, on decode and encode alike.
        let options: IndexOption =
            serde_json::from_str(r#"{"type": "FULLTEXT", "fulltext.analyzer": "whitespace"}"#)
                .unwrap();
        let encoded = serde_json::to_value(&options).unwrap();
        assert_eq!(
            encoded,
            serde_json::json!({"type": "FULLTEXT", "fulltext.analyzer": "whitespace"})
        );

        let options: IndexOption =
            serde_json::from_str(r#"{"type": "POINT", "spatial.wgs-84.min": [0.0, 40.0]}"#)
                .unwrap();
        let encoded = serde_json::to_value(&options).unwrap();
        assert_eq!(
            encoded,
            serde_json::json!({"type": "POINT", "spatial.wgs-84.min": [0.0, 40.0]})
        );

        let options: IndexOption = serde_json::from_str(r#"{"type": "VECTOR"}"#).unwrap();
        assert_eq!(
            serde_json::to_value(&options).unwrap(),
            serde_json::json!({"type": "VECTOR"})
        );
    }
}
