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

//! Converts a model between its longhand and shorthand notations.
//!
//! Shorthand is a more concise way of writing parts of a model on input; a model is always output
//! in longhand:
//! - a node's `label` is shorthand for `labels.identifier`
//! - a property's `key`, `unique` and `must_exist` are shorthand for constraints on that property
//!
//! [longhand] expands shorthand; decoded models are always expanded, so every document form of
//! the same model encodes the same way.

use indexmap::IndexMap;

use super::constraint::Constraint;
use super::name_format::constraint_name;
use super::property::Property;

use super::types::ConstraintType;
use super::GraphModel;

/// Expands any shorthand in the model into its longhand form.
pub fn longhand(model: &mut GraphModel) {
    for (entity, node) in &mut model.nodes {
        let label = node.label.clone();
        if label.is_some() && node.labels.identifier.is_none() {
            node.labels.identifier = label.clone();
        }
        if label == node.labels.identifier {
            node.label = None;
        }
        let identifier = node.labels.identifier.clone().unwrap_or_default();
        expand(
            &mut node.constraints,
            &mut node.properties,
            entity,
            &identifier,
        );
    }
    for (entity, relationship) in &mut model.relationships {
        let label = relationship.r#type.clone();
        expand(
            &mut relationship.constraints,
            &mut relationship.properties,
            entity,
            &label,
        );
    }
}

/// The id a shorthand constraint expanded for `entity`, `constraint_type` and `properties` gets,
/// the same id every time, so a shorthand document and its longhand form agree.
pub fn deterministic_id(
    entity: &str,
    constraint_type: ConstraintType,
    properties: &[&str],
) -> String {
    let mut distinct: Vec<&str> = properties.to_vec();
    distinct.sort_unstable();
    distinct.dedup();
    format!(
        "{}_{}_{}",
        constraint_type.lowercase(),
        entity,
        distinct.join("_")
    )
}

/// Converts shorthand property constraints into longhand constraints.
///
/// Doesn't check or transform them (e.g. for overlapping).
fn expand(
    constraints: &mut IndexMap<String, Constraint>,
    properties: &mut IndexMap<String, Property>,
    entity: &str,
    label: &str,
) {
    for (property_id, property) in properties.iter_mut() {
        let token = property.name.clone().unwrap_or_else(|| property_id.clone());
        let add = |constraint_type: ConstraintType,
                   constraints: &mut IndexMap<String, Constraint>| {
            let mut created = Constraint {
                r#type: constraint_type,
                properties: vec![property_id.to_owned()],
                extensions: Default::default(),
                name: None,
            };
            let name = constraint_name(&[&token], label, constraint_type);
            if !name.is_empty() {
                created.name = Some(name);
            }
            constraints.insert(
                deterministic_id(entity, constraint_type, &[&token]),
                created,
            );
        };

        // Only a set shorthand is expanded, and only then is the flag cleared: an explicit
        // false stays in the model, as in the reference implementation.
        if property.key == Some(true) {
            property.key = None;
            add(ConstraintType::Key, constraints);
        }
        if property.unique == Some(true) {
            property.unique = None;
            add(ConstraintType::Unique, constraints);
        }
        if property.must_exist == Some(true) {
            property.must_exist = None;
            add(ConstraintType::Exists, constraints);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::model::node::{Labels, Node};
    use crate::model::relationship::{Relationship, RelationshipTarget};
    use crate::model::GraphModel;
    use indexmap::IndexMap;

    fn model_with_shorthand() -> GraphModel {
        let mut person = Node {
            label: Some("Person".to_owned()),
            ..Default::default()
        };
        person.properties.insert(
            "name".to_owned(),
            Property {
                must_exist: Some(true),
                unique: Some(true),
                ..Default::default()
            },
        );
        let mut knows = Relationship {
            r#type: "KNOWS".to_owned(),
            from: RelationshipTarget::default(),
            to: RelationshipTarget::default(),
            properties: IndexMap::new(),
            constraints: IndexMap::new(),
            extensions: Default::default(),
            description: String::new(),
            aliases: Vec::new(),
            reference: String::new(),
        };
        knows.properties.insert(
            "since".to_owned(),
            Property {
                key: Some(true),
                name: Some("since_date".to_owned()),
                ..Default::default()
            },
        );
        GraphModel {
            nodes: IndexMap::from([("person".to_owned(), person)]),
            relationships: IndexMap::from([("knows".to_owned(), knows)]),
            ..Default::default()
        }
    }

    #[test]
    fn expands_shorthand_into_longhand() {
        let mut model = model_with_shorthand();

        longhand(&mut model);

        let person = &model.nodes["person"];
        assert_eq!(person.label, None);
        assert_eq!(person.labels.identifier, Some("Person".to_owned()));
        assert_eq!(person.properties["name"].must_exist, None);
        let names: Vec<&str> = person.constraints.keys().map(|k| k.as_str()).collect();
        assert_eq!(names, ["unique_person_name", "exists_person_name"]);
        assert_eq!(
            person.constraints["unique_person_name"].name,
            Some("name_Person_uniq".to_owned())
        );

        let knows = &model.relationships["knows"];
        assert_eq!(knows.properties["since"].key, None);
        assert_eq!(
            knows.constraints["key_knows_since_date"].properties,
            ["since"]
        );
        assert_eq!(
            knows.constraints["key_knows_since_date"].name,
            Some("since__date_KNOWS_key".to_owned())
        );
    }

    #[test]
    fn keeps_a_label_that_differs_from_the_identifier() {
        let node = Node {
            label: Some("Alias".to_owned()),
            labels: Labels {
                identifier: Some("Real".to_owned()),
                ..Default::default()
            },
            ..Default::default()
        };
        let mut model = GraphModel::default();
        model.nodes.insert("n".to_owned(), node.clone());
        longhand(&mut model);
        assert_eq!(model.nodes["n"].label, Some("Alias".to_owned()));
        assert_eq!(model.nodes["n"].labels.identifier, Some("Real".to_owned()));
        assert_eq!(model.nodes["n"], node);
    }

    #[test]
    fn keeps_explicitly_false_shorthand_flags() {
        // The reference nulls a flag only when it is set: an explicit false is data, and a
        // document carrying it round-trips byte for byte.
        let mut person = Node {
            label: Some("Person".to_owned()),
            ..Default::default()
        };
        person.properties.insert(
            "name".to_owned(),
            Property {
                key: Some(false),
                unique: Some(false),
                must_exist: Some(false),
                ..Default::default()
            },
        );
        person.properties.insert(
            "id".to_owned(),
            Property {
                key: Some(true),
                ..Default::default()
            },
        );
        let mut model = GraphModel::default();
        model.nodes.insert("person".to_owned(), person);

        longhand(&mut model);

        let name = &model.nodes["person"].properties["name"];
        assert_eq!(name.key, Some(false));
        assert_eq!(name.unique, Some(false));
        assert_eq!(name.must_exist, Some(false));

        let id = &model.nodes["person"].properties["id"];
        assert_eq!(id.key, None);
        // Only id's set shorthand expanded: no unique_ or exists_ constraint exists for name.
        assert_eq!(
            model.nodes["person"].constraints.keys().collect::<Vec<_>>(),
            ["key_person_id"]
        );
    }
}
