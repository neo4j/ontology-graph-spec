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

//! Builds constraint and index names in the importer's expected form, `<property>_<label>_<suffix>`.

use super::types::{ConstraintType, IndexType};

/// The longest name built, matching what the database accepts.
const MAX_LENGTH: usize = 800;

/// The database name of a constraint on `properties` of `entity`, e.g. `name_Person_key`.
pub fn constraint_name(
    properties: &[&str],
    entity: &str,
    constraint_type: ConstraintType,
) -> String {
    let suffix = match constraint_type {
        ConstraintType::Unique => "uniq",
        ConstraintType::Key => "key",
        ConstraintType::Exists => "exists",
        ConstraintType::PropertyType => "type",
    };
    build(properties, entity, Some(suffix))
}

/// The database name of an index on `properties` of `entity`, e.g. `name_Person_range`.
pub fn index_name(properties: &[&str], entity: &str, index_type: Option<IndexType>) -> String {
    let suffix = index_type.map(|index_type| index_type.as_str().to_lowercase());
    build(properties, entity, suffix.as_deref())
}

fn build(properties: &[&str], entity: &str, suffix: Option<&str>) -> String {
    let property_part = properties
        .iter()
        .map(|property| escape(property))
        .collect::<Vec<_>>()
        .join("_");
    if property_part.is_empty() {
        return String::new();
    }
    let base = if entity.is_empty() {
        property_part
    } else {
        format!("{property_part}_{}", escape(entity))
    };
    let suffix_part = suffix
        .map(|suffix| format!("_{suffix}"))
        .unwrap_or_default();
    // The reference truncates by UTF-16 code units (JVM String.take); the suffixes are ASCII, so
    // counting its characters is the same as counting its units.
    let limit = MAX_LENGTH.saturating_sub(suffix_part.chars().count());
    let base = truncate_utf16(&base, limit);
    format!("{base}{suffix_part}")
}

/// The longest character-boundary prefix of [text] that fits in [limit] UTF-16 code units. The
/// reference cuts by UTF-16 units directly, so a surrogate pair straddling the limit is cut
/// mid-pair there; a Rust string cannot hold half a pair, so this cuts before it — the one
/// unrepresentable difference, on names that are 800 units long to the character.
fn truncate_utf16(text: &str, limit: usize) -> &str {
    let mut units = 0;
    for (index, character) in text.char_indices() {
        units += character.len_utf16();
        if units > limit {
            return &text[..index];
        }
    }
    text
}

/// An underscore inside a token is doubled, so the parts stay recoverable. Without it `a_b` on
/// label `c` collides with `a` on label `b_c`. A token that starts or ends with an underscore is
/// still ambiguous.
fn escape(token: &str) -> String {
    token.replace('_', "__")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn builds_constraint_names() {
        assert_eq!(
            constraint_name(&["accountNumber"], "Account", ConstraintType::Key),
            "accountNumber_Account_key"
        );
        assert_eq!(
            constraint_name(&["a_b"], "c", ConstraintType::Exists),
            "a__b_c_exists"
        );
        assert_eq!(constraint_name(&[], "Account", ConstraintType::Key), "");
    }

    #[test]
    fn truncates_long_names() {
        let property = "p".repeat(900);
        let name = constraint_name(&[&property], "Label", ConstraintType::Key);
        assert!(name.len() <= MAX_LENGTH);
        assert!(name.ends_with("_key"));
    }

    #[test]
    fn truncates_by_utf16_units_like_the_reference() {
        // Two units per emoji: 400 emoji are 800 UTF-16 units, so a key name on them is exactly
        // at the limit with its suffix dropped, and one more emoji is cut whole, not mid-pair.
        let emoji: String = "🎬".repeat(400);
        let name = constraint_name(&[&emoji], "", ConstraintType::Key);
        assert_eq!(name.encode_utf16().count(), MAX_LENGTH);
        assert!(name.ends_with("_key"));

        let longer: String = "🎬".repeat(500);
        let name = constraint_name(&[&longer], "", ConstraintType::Key);
        assert_eq!(name.encode_utf16().count(), MAX_LENGTH);
        assert!(name.ends_with("_key"));
        assert!(!name.contains('\u{FFFD}'));
    }
}
