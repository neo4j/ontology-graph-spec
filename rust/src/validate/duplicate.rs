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

//! Finds the repeated entries of an ordered collection that must not repeat, such as aliases or
//! constraint properties. Shared by every validator covering a list which used to be a set, so
//! they all report in the same way.
//!
//! One issue for each entry that repeats an earlier one, located by its index in the list at
//! `path`, so the first occurrence is left alone and the repeats are what get flagged.

use std::collections::HashSet;

use super::Issue;

/// The issues for the entries of `values` that repeat an earlier one. The repeated value is
/// attached to each issue as the `value` detail.
pub(crate) fn duplicate_entry_issues(
    code: &str,
    kind: &str,
    owner: &str,
    values: &[String],
    path: &str,
) -> Vec<Issue> {
    let mut seen = HashSet::new();
    let mut issues = Vec::new();
    for (index, value) in values.iter().enumerate() {
        if !seen.insert(value.clone()) {
            issues.push(Issue::with_details(
                code,
                format!("Duplicate {kind} '{value}' on {owner}"),
                format!("{path}/{index}"),
                [("value", value.clone())],
            ));
        }
    }
    issues
}
