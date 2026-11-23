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

//! A mapping's `depends_on` must reference the `id` of another mapping in the same list. Ids must
//! be unique within their list and the dependencies free of cycles so an import order can be
//! derived.

use std::collections::{BTreeMap, HashMap, HashSet};

use crate::model::GraphModel;

use super::{ptr, Issue, Validation};

pub(crate) struct MappingDependencies;

impl Validation for MappingDependencies {
    fn name(&self) -> &'static str {
        "MappingDependencies"
    }

    fn validate(&self, model: &GraphModel, issues: &mut Vec<Issue>) {
        let lists = &model.extensions.data_source_mappings;
        for (list_id, mappings) in lists {
            validate_list(list_id, mappings, lists, issues);
        }
    }
}

fn validate_list(
    list_id: &str,
    mappings: &[crate::model::mapping::Mapping],
    lists: &indexmap::IndexMap<String, Vec<crate::model::mapping::Mapping>>,
    issues: &mut Vec<Issue>,
) {
    let mut index_by_id: HashMap<&str, usize> = HashMap::new();
    for (index, mapping) in mappings.iter().enumerate() {
        let id = mapping.id();
        if id.is_empty() {
            continue;
        }
        match index_by_id.entry(id) {
            std::collections::hash_map::Entry::Occupied(_) => issues.push(Issue::new(
                "mapping_id_duplicate",
                format!("Mapping id '{id}' is used by more than one mapping in '{list_id}'"),
                ptr!["extensions", "data_source_mappings", list_id, index, "id"],
            )),
            std::collections::hash_map::Entry::Vacant(slot) => {
                slot.insert(index);
            }
        }
    }

    // Each mapping depends on at most one other, so follow the chain from every mapping looking
    // for a loop.
    let mut next: BTreeMap<usize, usize> = BTreeMap::new();
    for (index, mapping) in mappings.iter().enumerate() {
        let depends_on = mapping.depends_on();
        if depends_on.is_empty() {
            continue;
        }
        let target = index_by_id.get(depends_on).copied();
        let path = ptr![
            "extensions",
            "data_source_mappings",
            list_id,
            index,
            "depends_on"
        ];
        match target {
            Some(target) if target == index => issues.push(Issue::new(
                "mapping_depends_on_self",
                format!("Mapping '{depends_on}' can't depend on itself"),
                path,
            )),
            Some(target) => {
                next.insert(index, target);
            }
            None => {
                let in_other_list = lists.iter().any(|(other_id, other)| {
                    other_id != list_id && other.iter().any(|mapping| mapping.id() == depends_on)
                });
                if in_other_list {
                    issues.push(Issue::new(
                        "mapping_depends_on_other_list",
                        format!(
                            "Mapping depends on '{depends_on}' which is in a different list of \
                             mappings, mappings can only depend on others in the same list"
                        ),
                        path,
                    ));
                } else {
                    issues.push(Issue::new(
                        "mapping_depends_on_unknown",
                        format!(
                            "Mapping depends on '{depends_on}' which isn't the id of any mapping \
                             in '{list_id}'"
                        ),
                        path,
                    ));
                }
            }
        }
    }
    report_cycles(list_id, &next, issues);
}

fn report_cycles(list_id: &str, next: &BTreeMap<usize, usize>, issues: &mut Vec<Issue>) {
    let mut done: HashSet<usize> = HashSet::new();
    for start in next.keys().copied().collect::<Vec<_>>() {
        if done.contains(&start) {
            continue;
        }
        let mut path: Vec<usize> = Vec::new();
        let mut current = Some(start);
        while let Some(node) = current {
            if done.contains(&node) || path.contains(&node) {
                break;
            }
            path.push(node);
            current = next.get(&node).copied();
        }
        if let Some(node) = current {
            if let Some(position) = path.iter().position(|p| *p == node) {
                let cycle = &path[position..];
                let names = {
                    let mut sorted = cycle.to_vec();
                    sorted.sort_unstable();
                    sorted
                        .iter()
                        .map(|index| format!("[{index}]"))
                        .collect::<Vec<_>>()
                        .join(", ")
                };
                // The position was just found in path, so the cycle is non-empty by construction.
                let first = *cycle
                    .iter()
                    .min()
                    .expect("a found position yields a non-empty cycle");
                issues.push(Issue::new(
                    "mapping_dependency_cycle",
                    format!("Mappings {names} in '{list_id}' depend on each other in a cycle"),
                    ptr![
                        "extensions",
                        "data_source_mappings",
                        list_id,
                        first,
                        "depends_on"
                    ],
                ));
            }
        }
        done.extend(path);
    }
}
