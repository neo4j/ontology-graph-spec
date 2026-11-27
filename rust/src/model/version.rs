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

//! Released versions of the format and the schema urls that identify them.

/// Every released graph spec version, oldest first, ending with [LATEST].
pub const GRAPH_SPEC_VERSIONS: [&str; 2] = [V1, LATEST];

/// Graph spec version 1.0.0.
pub const V1: &str = "1.0.0";

/// Graph spec version 1.1.0.
pub const V1_1: &str = "1.1.0";

/// The latest released graph spec version.
pub const LATEST: &str = V1_1;

/// The base of every schema url; the spec version is always the path segment before schema.json.
pub const SCHEMA_BASE: &str = "https://neo4j.com/docs/ontology-graph-spec/";

/// The file name of the published schema.
pub const SCHEMA_FILE: &str = "schema.json";

/// The schema url of a released graph spec version.
pub fn schema_url(version: &str) -> String {
    format!("{SCHEMA_BASE}{version}/{SCHEMA_FILE}")
}

/// The schema url of [LATEST].
pub fn latest_schema_url() -> String {
    schema_url(LATEST)
}

/// The schema url without surrounding whitespace, trailing slashes and the schema file name —
/// the form both the version parser and the schema-url validator read, so decode and validation
/// agree on which urls are tolerated.
pub(crate) fn schema_base(schema: &str) -> &str {
    let base = schema.trim().trim_end_matches('/');
    let base = base.strip_suffix(SCHEMA_FILE).unwrap_or(base);
    base.trim_end_matches('/')
}

/// The spec version named by a schema url: the path segment before schema.json.
///
/// # Errors
///
/// When no version can be determined from the url.
pub fn parse_schema_version(schema: &str) -> Result<String, String> {
    let version = schema_base(schema).rsplit('/').next().unwrap_or_default();
    if version.is_empty() {
        return Err(format!(
            "Unable to determine version from schema '{schema}'"
        ));
    }
    Ok(version.to_owned())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_versions_from_urls() {
        assert_eq!(parse_schema_version(&schema_url("1.0.0")).unwrap(), "1.0.0");
        assert_eq!(
            parse_schema_version(" https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json/ ")
                .unwrap(),
            "1.1.0"
        );
        assert_eq!(
            parse_schema_version("https://example.com/2.0.0").unwrap(),
            "2.0.0"
        );
        assert_eq!(
            parse_schema_version("https://example.com/").unwrap(),
            "example.com"
        );
        assert!(parse_schema_version("").is_err());
    }
}
