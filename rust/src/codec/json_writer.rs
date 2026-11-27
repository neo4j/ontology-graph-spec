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

//! Pretty-prints JSON in the reference implementation's form: four-space indents, each entry of a
//! non-empty object or array on its own line, empty ones inline.

pub(crate) fn to_pretty_string(value: &serde_json::Value) -> String {
    let mut out = String::new();
    write_value(value, 0, &mut out);
    out
}

fn write_value(value: &serde_json::Value, indent: usize, out: &mut String) {
    match value {
        serde_json::Value::Null => out.push_str("null"),
        serde_json::Value::Bool(bool) => out.push_str(if *bool { "true" } else { "false" }),
        serde_json::Value::Number(number) => out.push_str(&number.to_string()),
        serde_json::Value::String(string) => write_string(string, out),
        serde_json::Value::Array(items) => {
            if items.is_empty() {
                out.push_str("[]");
            } else {
                out.push_str("[\n");
                for (index, item) in items.iter().enumerate() {
                    if index > 0 {
                        out.push_str(",\n");
                    }
                    out.push_str(&" ".repeat((indent + 1) * 4));
                    write_value(item, indent + 1, out);
                }
                out.push('\n');
                out.push_str(&" ".repeat(indent * 4));
                out.push(']');
            }
        }
        serde_json::Value::Object(entries) => {
            if entries.is_empty() {
                out.push_str("{}");
            } else {
                out.push_str("{\n");
                for (index, (key, value)) in entries.iter().enumerate() {
                    if index > 0 {
                        out.push_str(",\n");
                    }
                    out.push_str(&" ".repeat((indent + 1) * 4));
                    write_string(key, out);
                    out.push_str(": ");
                    write_value(value, indent + 1, out);
                }
                out.push('\n');
                out.push_str(&" ".repeat(indent * 4));
                out.push('}');
            }
        }
    }
}

fn write_string(string: &str, out: &mut String) {
    out.push('"');
    for c in string.chars() {
        match c {
            '"' => out.push_str("\\\""),
            '\\' => out.push_str("\\\\"),
            '\n' => out.push_str("\\n"),
            '\r' => out.push_str("\\r"),
            '\t' => out.push_str("\\t"),
            c if (c as u32) < 0x20 => out.push_str(&format!("\\u{:04x}", c as u32)),
            c => out.push(c),
        }
    }
    out.push('"');
}
