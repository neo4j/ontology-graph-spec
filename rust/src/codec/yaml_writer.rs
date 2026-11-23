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

//! Pretty-prints YAML from a JSON value, matching the reference implementation's writer:
//! two-space indents, flow style where the [print options](YamlPrintOptions) say so, and quoting
//! only where YAML needs it. The value is the document in JSON form, so the same tree writes the
//! JSON and YAML of a model and the two cannot disagree.

/// How the YAML writer prints: the indent of nested blocks, whether every string is quoted, and
/// the paths written in flow style.
pub(crate) struct YamlPrintOptions {
    /// The indent of nested mappings and sequences.
    pub indent: usize,
    /// Whether every string is quoted, not only those YAML would need to quote.
    pub always_quote_strings: bool,
    /// Path patterns (`*` for one key or index level, `**` for any depth) whose elements are
    /// written in flow style.
    pub inline_paths: &'static [&'static str],
}

impl YamlPrintOptions {
    /// The model format's options: two-space indents, every string quoted, and the paths the
    /// reference implementation writes in flow style.
    pub(crate) const fn model_format() -> YamlPrintOptions {
        YamlPrintOptions {
            indent: 2,
            always_quote_strings: true,
            inline_paths: INLINE_PATHS,
        }
    }
}

/// The paths the model format writes in flow style, mirroring the reference implementation's
/// `YamlFormat.default` options.
const INLINE_PATHS: &[&str] = &[
    "nodes.*.properties.*",
    "nodes.*.constraints.*.properties",
    "nodes.*.extensions.indexes.*.labels",
    "nodes.*.extensions.indexes.*.properties",
    "relationships.*.properties.*",
    "relationships.*.from",
    "relationships.*.to",
    "relationships.*.constraints.*.properties",
    "relationships.*.extensions.indexes.*.properties",
    "relationships.*.extensions.indexes.*.options.*",
    "extensions.data_source_tables.*.columns[*].supported",
    "extensions.data_source_tables.*.primary_keys",
    "extensions.data_source_tables.*.foreign_keys.*.columns",
    "extensions.data_source_tables.*.foreign_keys.*.references.columns",
    "extensions.data_source_mappings.*[*].properties.*",
    "extensions.data_source_mappings.*[*].key",
    "extensions.data_source_mappings.*[*].from_node.properties.*",
    "extensions.data_source_mappings.*[*].to_node.properties.*",
];

/// A path pattern patterns into matchable tokens: `*` matches one key or index level (any
/// characters but `.`), `**` matches across any depth, everything else is literal.
enum Token {
    Literal(String),
    OneLevel,
    AnyDepth,
}

/// The patterns of one [write], tokenized once: the exact strings beside their tokens, so the
/// compiled form carries everything a match needs. The exact comparison beside the token match is
/// the reference's own disjunct, and covers keys containing dots, which a `*` segment's token
/// cannot match.
struct CompiledPatterns {
    exact: &'static [&'static str],
    tokens: Vec<Vec<Token>>,
}

impl CompiledPatterns {
    fn new(options: &YamlPrintOptions) -> CompiledPatterns {
        CompiledPatterns {
            exact: options.inline_paths,
            tokens: options.inline_paths.iter().map(|p| compile(p)).collect(),
        }
    }

    /// Whether the element at `path` is written in flow style: an exact pattern match, or a
    /// token match.
    fn matches(&self, path: &str) -> bool {
        self.exact
            .iter()
            .zip(&self.tokens)
            .any(|(pattern, tokens)| *pattern == path || matches_tokens(tokens, path))
    }
}

fn compile(pattern: &str) -> Vec<Token> {
    let mut tokens = Vec::new();
    let mut literal = String::new();
    let mut chars = pattern.chars().peekable();
    while let Some(c) = chars.next() {
        if c == '*' {
            let double = chars.peek() == Some(&'*');
            if double {
                chars.next();
                push_literal(&mut tokens, &mut literal);
                tokens.push(Token::AnyDepth);
            } else {
                push_literal(&mut tokens, &mut literal);
                tokens.push(Token::OneLevel);
            }
        } else {
            literal.push(c);
        }
    }
    push_literal(&mut tokens, &mut literal);
    tokens
}

fn push_literal(tokens: &mut Vec<Token>, literal: &mut String) {
    if !literal.is_empty() {
        tokens.push(Token::Literal(std::mem::take(literal)));
    }
}

fn matches_tokens(tokens: &[Token], path: &str) -> bool {
    match tokens.split_first() {
        None => path.is_empty(),
        Some((token, rest)) => match token {
            Token::Literal(literal) => {
                path.starts_with(literal.as_str()) && matches_tokens(rest, &path[literal.len()..])
            }
            Token::OneLevel => {
                // One or more characters without a '.', then the rest of the tokens. The possible
                // cut points are before the first '.', longest first.
                let mut ends = Vec::new();
                for (index, c) in path.char_indices() {
                    if c == '.' {
                        break;
                    }
                    ends.push(index + c.len_utf8());
                }
                ends.iter()
                    .rev()
                    .any(|&split| matches_tokens(rest, &path[split..]))
            }
            Token::AnyDepth => (0..=path.len())
                .rev()
                .any(|split| path.is_char_boundary(split) && matches_tokens(rest, &path[split..])),
        },
    }
}

/// The path of a child of the element at `path`, named `identifier`: a key or `[i]`.
fn child_path(path: &str, identifier: &str) -> String {
    if path.is_empty() {
        identifier.to_owned()
    } else if identifier.starts_with('[') {
        format!("{path}{identifier}")
    } else {
        format!("{path}.{identifier}")
    }
}

/// Writes the value as a YAML document: a block-style tree with flow style where the options
/// say so, ending in a single newline.
/// The indent step of 2 in the list branch is hardcoded, not [YamlPrintOptions::indent]: the
/// reference writer does the same, and the spec tests byte-compare its output.
pub(crate) fn write(value: &serde_json::Value, options: &YamlPrintOptions) -> String {
    let patterns = CompiledPatterns::new(options);
    let mut out = String::new();
    write_element(value, "", 0, false, options, &patterns, &mut out);
    let trimmed = out.trim_end();
    trimmed.to_owned() + "\n"
}

fn write_element(
    value: &serde_json::Value,
    path: &str,
    indent: usize,
    force_inline: bool,
    options: &YamlPrintOptions,
    patterns: &CompiledPatterns,
    out: &mut String,
) {
    let inline = force_inline || patterns.matches(path);
    match value {
        serde_json::Value::Null => out.push_str("null"),
        serde_json::Value::Bool(bool) => out.push_str(if *bool { "true" } else { "false" }),
        serde_json::Value::Number(number) => out.push_str(canonical_number(&number.to_string())),
        serde_json::Value::String(string) => {
            out.push_str(&escape_and_quote(string, options.always_quote_strings));
        }
        serde_json::Value::Array(items) => {
            if items.is_empty() {
                out.push_str("[]");
            } else if inline {
                out.push('[');
                for (index, child) in items.iter().enumerate() {
                    if index > 0 {
                        out.push_str(", ");
                    }
                    let child_path = child_path(path, &format!("[{index}]"));
                    write_element(child, &child_path, indent, true, options, patterns, out);
                }
                out.push(']');
            } else {
                for (index, child) in items.iter().enumerate() {
                    if index > 0 {
                        out.push('\n');
                        out.push_str(&" ".repeat(indent));
                    }
                    out.push_str("- ");
                    let child_path = child_path(path, &format!("[{index}]"));
                    let child_inline = patterns.matches(&child_path);
                    match child {
                        // A non-inline, nonempty map opens on the list item's own line: the
                        // reference writer's list form, byte-compared by the spec tests. An
                        // empty map goes through write_element, which writes `{}` — a bare `-`
                        // would decode as null.
                        serde_json::Value::Object(entries)
                            if !child_inline && !entries.is_empty() =>
                        {
                            write_block_map(
                                entries,
                                &child_path,
                                indent + 2,
                                options,
                                patterns,
                                out,
                            );
                        }
                        // Everything else is written one level deeper; a non-inline list puts
                        // itself on the next line first.
                        _ => {
                            if matches!(child, serde_json::Value::Array(_)) && !child_inline {
                                out.push('\n');
                                out.push_str(&" ".repeat(indent + 2));
                            }
                            write_element(
                                child,
                                &child_path,
                                indent + 2,
                                false,
                                options,
                                patterns,
                                out,
                            );
                        }
                    }
                }
            }
        }
        serde_json::Value::Object(entries) => {
            if entries.is_empty() {
                out.push_str("{}");
            } else if inline {
                out.push_str("{ ");
                for (index, (key, value)) in entries.iter().enumerate() {
                    if index > 0 {
                        out.push_str(", ");
                    }
                    out.push_str(&escape_and_quote(key, false));
                    out.push_str(": ");
                    let child_path = child_path(path, key);
                    write_element(value, &child_path, indent, true, options, patterns, out);
                }
                out.push_str(" }");
            } else {
                write_block_map(entries, path, indent, options, patterns, out);
            }
        }
    }
}

fn write_block_map(
    entries: &serde_json::Map<String, serde_json::Value>,
    path: &str,
    indent: usize,
    options: &YamlPrintOptions,
    patterns: &CompiledPatterns,
    out: &mut String,
) {
    let mut first = true;
    for (key, child) in entries {
        if !first {
            out.push('\n');
            out.push_str(&" ".repeat(indent));
        }
        first = false;
        out.push_str(&escape_and_quote(key, false));
        out.push(':');
        let child_path = child_path(path, key);
        let child_inline = patterns.matches(&child_path);
        match child {
            serde_json::Value::Object(_) | serde_json::Value::Array(_) if !child_inline => {
                out.push('\n');
                out.push_str(&" ".repeat(indent + options.indent));
                write_element(
                    child,
                    &child_path,
                    indent + options.indent,
                    false,
                    options,
                    patterns,
                    out,
                );
            }
            _ => {
                out.push(' ');
                write_element(child, &child_path, indent, false, options, patterns, out);
            }
        }
    }
}

/// Kotlin/JVM writes a whole double as `33.0` and Kotlin/JS as `33`. Drop the `.0` so both write
/// the same YAML.
fn canonical_number(value: &str) -> &str {
    let body = value.strip_prefix('-').unwrap_or(value);
    if let Some(digits) = body.strip_suffix(".0") {
        if !digits.is_empty() && digits.bytes().all(|byte| byte.is_ascii_digit()) {
            return &value[..value.len() - 2];
        }
    }
    value
}

fn needs_quotes(value: &str) -> bool {
    value.is_empty()
        || value.trim() != value
        || value.chars().any(|c| ":{}[],&*#?|-<>=!%@`\"'".contains(c))
        || value == "true"
        || value == "false"
        || value == "null"
        || value.contains('\n')
        || value.contains('\t')
        || value.contains('\r')
        || value.chars().any(needs_escape)
        || parses_as_java_double(value)
}

/// Whether a character is non-printable in YAML and so must be written as an escape.
fn needs_escape(c: char) -> bool {
    (c < ' ' && !matches!(c, '\t' | '\n' | '\r')) || ('\u{7f}'..='\u{9f}').contains(&c)
}

/// Whether the reference's number acceptance (Kotlin `toDoubleOrNull`, Java `Double.parseDouble`)
/// accepts the value: decimal notation with an optional `f`/`F`/`d`/`D` suffix, or exactly
/// `Infinity`/`NaN` with an optional sign. Rust's parser additionally accepts `inf`, `infinity`
/// and `nan` in any case, which the reference leaves as plain strings — so those must not quote.
fn parses_as_java_double(value: &str) -> bool {
    let candidate = match value.as_bytes() {
        [body @ .., b'f' | b'F' | b'd' | b'D'] if !body.is_empty() => &value[..value.len() - 1],
        _ => value,
    };
    let Ok(parsed) = candidate.parse::<f64>() else {
        return false;
    };
    if parsed.is_finite() {
        return true;
    }
    let body = candidate.strip_prefix(['+', '-']).unwrap_or(candidate);
    body == "Infinity" || body == "NaN"
}

fn escape_and_quote(value: &str, force_quotes: bool) -> String {
    if !force_quotes && !needs_quotes(value) {
        return value.to_owned();
    }
    let mut escaped = String::with_capacity(value.len());
    for c in value.chars() {
        match c {
            '\\' => escaped.push_str("\\\\"),
            '"' => escaped.push_str("\\\""),
            '\n' => escaped.push_str("\\n"),
            '\r' => escaped.push_str("\\r"),
            '\t' => escaped.push_str("\\t"),
            // Non-printable characters must be escaped or the document isn't valid YAML.
            '\0' => escaped.push_str("\\0"),
            '\u{7}' => escaped.push_str("\\a"),
            '\u{8}' => escaped.push_str("\\b"),
            '\u{b}' => escaped.push_str("\\v"),
            '\u{c}' => escaped.push_str("\\f"),
            '\u{1b}' => escaped.push_str("\\e"),
            c if needs_escape(c) && c <= '\u{ff}' => {
                escaped.push_str(&format!("\\x{:02X}", c as u32))
            }
            c if needs_escape(c) => escaped.push_str(&format!("\\u{:04X}", c as u32)),
            c => escaped.push(c),
        }
    }
    format!("\"{escaped}\"")
}

#[cfg(test)]
mod tests {
    use super::*;

    fn options(inline_paths: &'static [&'static str]) -> YamlPrintOptions {
        YamlPrintOptions {
            indent: 2,
            always_quote_strings: true,
            inline_paths,
        }
    }

    #[test]
    fn matches_path_patterns() {
        assert!(matches_tokens(
            &compile("nodes.*.properties.*"),
            "nodes.Person.properties.id"
        ));
        assert!(!matches_tokens(
            &compile("nodes.*.properties.*"),
            "nodes.Person.properties.dotted.id"
        ));
        assert!(matches_tokens(&compile("a**b"), "a.x.y.b"));
        assert!(matches_tokens(&compile("list[*].x"), "list[0].x"));
        assert!(!matches_tokens(&compile("nodes.*.properties.*"), "nodes"));
        assert!(matches_tokens(&compile("*"), "one"));
        assert!(!matches_tokens(&compile("*"), "a.b"));
    }

    #[test]
    fn writes_nested_maps_and_lists() {
        let value = serde_json::json!({
            "list": ["a", 1]
        });
        assert_eq!(write(&value, &options(&[])), "list:\n  - \"a\"\n  - 1\n");
    }

    #[test]
    fn inlines_matching_paths() {
        let value = serde_json::json!({
            "nodes": {"person": {"from": {"node": "x"}}}
        });
        assert_eq!(
            write(&value, &options(&["nodes.*.from"])),
            "nodes:\n  person:\n    from: { node: \"x\" }\n"
        );
    }

    #[test]
    fn drops_trailing_point_zero_from_whole_doubles() {
        assert_eq!(canonical_number("40.0"), "40");
        assert_eq!(canonical_number("-40.0"), "-40");
        assert_eq!(canonical_number("4.5"), "4.5");
        assert_eq!(canonical_number("40"), "40");
    }

    #[test]
    fn quotes_only_what_yaml_needs() {
        assert_eq!(escape_and_quote("minimal", false), "minimal");
        assert_eq!(escape_and_quote("123", false), "\"123\"");
        assert_eq!(escape_and_quote("true", false), "\"true\"");
        assert_eq!(escape_and_quote("a: b", false), "\"a: b\"");
        assert_eq!(escape_and_quote("", false), "\"\"");
        assert_eq!(escape_and_quote("line\nbreak", false), "\"line\\nbreak\"");
    }

    #[test]
    fn quotes_numbers_only_as_the_reference_accepts_them() {
        // The reference reads numbers through Java's Double.parseDouble: inf/infinity/nan in any
        // other spelling are plain strings and stay unquoted; Infinity/NaN and suffixed decimals
        // are numbers and are quoted.
        assert_eq!(escape_and_quote("inf", false), "inf");
        assert_eq!(escape_and_quote("INF", false), "INF");
        assert_eq!(escape_and_quote("nan", false), "nan");
        assert_eq!(escape_and_quote("Infinity", false), "\"Infinity\"");
        assert_eq!(escape_and_quote("-Infinity", false), "\"-Infinity\"");
        assert_eq!(escape_and_quote("NaN", false), "\"NaN\"");
        assert_eq!(escape_and_quote("1f", false), "\"1f\"");
        assert_eq!(escape_and_quote("0x1F", false), "0x1F");
        assert_eq!(escape_and_quote("1e5", false), "\"1e5\"");
    }
}
