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

//! The model's `schema` url must be the url of a released graph spec version. Surrounding
//! whitespace, trailing slashes and a missing `schema.json` are tolerated, as they are by
//! [parse_schema_version](crate::model::version::parse_schema_version).

use super::{ptr, Issue, Validation};
use crate::model::version::{self, GRAPH_SPEC_VERSIONS, SCHEMA_BASE};
use crate::model::GraphModel;

pub(crate) struct SchemaUrl;

impl Validation for SchemaUrl {
    fn name(&self) -> &'static str {
        "SchemaUrl"
    }

    fn validate(&self, model: &GraphModel, issues: &mut Vec<Issue>) {
        let schema = &model.schema;
        let base = version::schema_base(schema);
        let released = GRAPH_SPEC_VERSIONS
            .iter()
            .any(|version| format!("{SCHEMA_BASE}{version}") == base);
        if !released {
            issues.push(Issue::new(
                "invalid_schema_url",
                format!("Schema '{schema}' is not the url of a released graph spec schema"),
                ptr!["schema"],
            ));
        }
    }
}
