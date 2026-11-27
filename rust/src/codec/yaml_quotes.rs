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

//! Re-quotes the plain (unquoted) scalars a standard YAML parser would resolve to null, a boolean
//! or a special float, but the reference implementation reads as strings.
//!
//! The reference YAML library resolves only the lowercase `true`/`false`, the exact `null`, `~`
//! and an empty value as typed scalars (plus its number grammar); every other unquoted spelling
//! stays a string, so `identifier: Null` is the string "Null". A standard YAML 1.2 core parser
//! also resolves `Null`, `NULL`, `TRUE`, `.inf`, hex and octal integers, leading `+` and friends,
//! which would lose that string. Where a scalar starts, if it is one of those spellings and
//! stands alone, it is wrapped in quotes so it parses back to the same string the reference
//! library reads.
//!
//! A scalar only starts where a node can start: at the start of a line or after an indicator
//! (`: `, `- `, `? `, `[`, `{`, `,` in flow collections). Plain scalars inside comments, quoted
//! scalars and block scalars (`|` and `>`) are left alone. The scanner is a port of the reference
//! implementation's own scalar-marking pass, which is battle-tested against the same corpus.

/// Whether a plain scalar is typed by the standard YAML parser but read as a string by the
/// reference, and so must be re-quoted.
fn needs_requote(scalar: &str) -> bool {
    standard_types(scalar) && !reference_types(scalar)
}

/// Whether the reference reads the scalar as a typed value: the exact `true`/`false`, `null`,
/// `~` and empty spellings, and its number grammar (`-?(0|[1-9][0-9]*)(\.[0-9]+)?([eE][-+]?[0-9]+)?`).
fn reference_types(scalar: &str) -> bool {
    matches!(scalar, "true" | "false" | "null" | "~" | "") || reference_number(scalar)
}

/// The reference's number grammar, full match.
fn reference_number(scalar: &str) -> bool {
    let body = scalar.strip_prefix('-').unwrap_or(scalar);
    let digits = body.len() - body.trim_start_matches(|c: char| c.is_ascii_digit()).len();
    let rest = &body[digits..];
    if digits == 0 || (digits > 1 && body.starts_with('0')) {
        return false;
    }
    let rest = rest
        .strip_prefix('.')
        .map(|fraction| {
            let digits = fraction.len()
                - fraction
                    .trim_start_matches(|c: char| c.is_ascii_digit())
                    .len();
            if digits == 0 {
                return None;
            }
            Some(&fraction[digits..])
        })
        .unwrap_or(Some(rest));
    let Some(rest) = rest else { return false };
    match rest.strip_prefix(['e', 'E']) {
        Some(exponent) => {
            let exponent = exponent.strip_prefix(['-', '+']).unwrap_or(exponent);
            !exponent.is_empty() && exponent.bytes().all(|b| b.is_ascii_digit())
        }
        None => rest.is_empty(),
    }
}

/// Whether the standard YAML parser (serde_norway) resolves the scalar to a typed value.
fn standard_types(scalar: &str) -> bool {
    matches!(
        scalar,
        "null"
            | "Null"
            | "NULL"
            | "~"
            | ""
            | "true"
            | "True"
            | "TRUE"
            | "false"
            | "False"
            | "FALSE"
    ) || standard_int(scalar)
        || standard_float(scalar)
}

/// The standard parser's integer grammar: an optional sign, then decimal (no leading zeros),
/// `0x` hex, `0o` octal or `0b` binary.
fn standard_int(scalar: &str) -> bool {
    let body = scalar.strip_prefix(['-', '+']).unwrap_or(scalar);
    if body.is_empty() || body.starts_with(['-', '+']) {
        return false;
    }
    for (prefix, radix) in [("0x", 16), ("0o", 8), ("0b", 2)] {
        if let Some(digits) = body.strip_prefix(prefix) {
            return !digits.is_empty()
                && !digits.starts_with(['-', '+'])
                && digits.bytes().all(|b| (b as char).is_digit(radix));
        }
    }
    // Decimal: `0` alone or a non-zero-leading run of digits.
    body.bytes().all(|b| b.is_ascii_digit()) && (body.len() == 1 || !body.starts_with('0'))
}

/// The standard parser's float grammar: a Rust-parseable finite float, or the signed and
/// case-varied `.inf`/`.nan` spellings.
fn standard_float(scalar: &str) -> bool {
    // Signed spellings first: they carry a sign the check below must not reject.
    if matches!(
        scalar,
        "-.inf" | "-.Inf" | "-.INF" | ".nan" | ".NaN" | ".NAN"
    ) {
        return true;
    }
    let unpositive = scalar.strip_prefix('+').unwrap_or(scalar);
    if unpositive.starts_with(['-', '+']) {
        return false;
    }
    if matches!(unpositive, ".inf" | ".Inf" | ".INF") {
        return true;
    }
    // Leading zeros make digits a string, not a number.
    let digits = unpositive.len()
        - unpositive
            .trim_start_matches(|c: char| c.is_ascii_digit())
            .len();
    if digits > 1 && unpositive.starts_with('0') {
        return false;
    }
    matches!(unpositive.parse::<f64>(), Ok(value) if value.is_finite())
}

pub(crate) fn requote_plain_scalars(yaml: &str) -> String {
    let mut scanner = Scanner::new(yaml);
    scanner.run();
    scanner.output
}

/// The raw text and a char index over it, with the output built alongside.
struct Scanner<'a> {
    yaml: &'a [u8],
    chars: Vec<(usize, char)>,
    output: String,
}

impl<'a> Scanner<'a> {
    fn new(yaml: &'a str) -> Scanner<'a> {
        Scanner {
            yaml: yaml.as_bytes(),
            chars: yaml.char_indices().collect(),
            output: String::with_capacity(yaml.len()),
        }
    }

    fn char(&self, pos: usize) -> Option<char> {
        self.chars.get(pos).map(|(_, c)| *c)
    }

    /// The byte index of the char at [pos], or the end of the text.
    fn index(&self, pos: usize) -> usize {
        self.chars
            .get(pos)
            .map(|(index, _)| *index)
            .unwrap_or(self.yaml.len())
    }

    /// The char position of the byte [index], or the end of the char index.
    fn pos(&self, index: usize) -> usize {
        self.chars
            .binary_search_by_key(&index, |(position, _)| *position)
            .unwrap_or_else(|position| position)
    }

    fn byte(&self, index: usize) -> Option<u8> {
        self.yaml.get(index).copied()
    }

    /// Whether the character at [index] ends an indicator; in flow collections `,`, `]` and `}`
    /// do as well.
    fn is_break(&self, index: usize, flow: usize) -> bool {
        match self.byte(index) {
            None => true,
            Some(b' ' | b'\t' | b'\n' | b'\r') => true,
            Some(b',' | b']' | b'}') => flow > 0,
            _ => false,
        }
    }

    fn is_whitespace_at(&self, index: usize) -> bool {
        matches!(self.byte(index), Some(b' ' | b'\t' | b'\n' | b'\r') | None)
    }

    /// The text between the byte indexes [start] and [end].
    fn slice(&self, start: usize, end: usize) -> String {
        String::from_utf8_lossy(&self.yaml[start..end]).into_owned()
    }

    fn run(&mut self) {
        let mut at_start = true; // The next significant character starts a node
        let mut in_plain = false; // Inside a plain (unquoted) scalar
        let mut pending_plain = false; // A plain scalar ended at a line break, may continue
        let mut plain_indent = 0; // The indent a continuation line must exceed to continue it
        let mut flow = 0; // Depth of nested `[` and `{`
        let mut line_start = 0; // Byte index of the start of the current line
        let mut at_line_start = true;
        let mut indent = 0; // Indent of the node the current scalar belongs to
        let mut scalar_column = 0;
        let mut pos = 0; // Position in the char index

        while let Some(c) = self.char(pos) {
            let index = self.index(pos);
            if c == '\n' {
                self.output.push('\n');
                pos += 1;
                line_start = self.index(pos);
                at_line_start = true;
                if flow == 0 {
                    if in_plain {
                        pending_plain = true;
                        plain_indent = indent;
                        in_plain = false;
                    } else if !pending_plain {
                        at_start = true;
                    }
                }
                continue;
            }
            if c == ' ' || c == '\t' || c == '\r' {
                self.output.push(c);
                pos += 1;
                continue;
            }
            if at_line_start {
                at_line_start = false;
                let column = index - line_start;
                if pending_plain && flow == 0 && column > plain_indent && c != '#' {
                    in_plain = true;
                } else {
                    at_start = flow == 0 || at_start;
                    if flow == 0 {
                        indent = column;
                    }
                }
                pending_plain = false;
            }
            if c == '#' && self.is_whitespace_at(index.wrapping_sub(1)) {
                let end = self.yaml[index..]
                    .iter()
                    .position(|b| *b == b'\n')
                    .map(|offset| index + offset)
                    .unwrap_or(self.yaml.len());
                self.output.push_str(&self.slice(index, end));
                pos = self.pos(end);
                continue;
            } else if in_plain && c == ':' && self.is_break(index + c.len_utf8(), flow) {
                in_plain = false;
                at_start = true;
                indent = scalar_column;
            } else if in_plain && flow > 0 && matches!(c, ',' | ']' | '}') {
                // Reprocess the flow indicator with the scalar ended, so it is handled as one
                in_plain = false;
                continue;
            } else if in_plain {
                // Inside a plain scalar: everything is part of it
            } else if at_start && (c == '"' || c == '\'') {
                scalar_column = index - line_start;
                self.output.push(c);
                pos = self.copy_quoted(pos + 1, c);
                at_start = false;
                continue;
            } else if at_start
                && flow == 0
                && (c == '|' || c == '>')
                && self.block_header_end(index).is_some()
            {
                pos = self.copy_block_scalar(pos, indent);
                line_start = self.index(pos);
                at_line_start = true;
                continue;
            } else if at_start && (c == '-' || c == '?') && self.is_break(index + 1, 0) {
                indent = index - line_start;
            } else if at_start && (c == '[' || c == '{') {
                flow += 1;
            } else if flow > 0 && (c == ']' || c == '}') {
                flow -= 1;
                at_start = false;
            } else if flow > 0 && c == ',' {
                at_start = true;
            } else if at_start && (c == '&' || c == '!') {
                // Anchors and tags are followed by the node they apply to
                while let Some(c) = self.char(pos) {
                    if c.is_whitespace() {
                        break;
                    }
                    self.output.push(c);
                    pos += 1;
                }
                continue;
            } else if at_start && c == ':' && self.is_break(index + c.len_utf8(), flow) {
                // An explicit mapping-key indicator: what follows it starts a node
            } else if at_start {
                // A plain scalar starts: if it is one of the re-quoted spellings and stands alone,
                // quote it. The rest of the scalar, including continuation lines, is copied
                // unchanged, one line at a time from here.
                let start_pos = pos;
                let mut end_pos = pos;
                let mut runs_to_line_end = false;
                while let Some(c) = self.char(end_pos) {
                    let index = self.index(end_pos);
                    match c {
                        '\n' => {
                            runs_to_line_end = true;
                            break;
                        }
                        ':' if self.is_break(index + c.len_utf8(), flow) => break,
                        ',' | ']' | '}' if flow > 0 => break,
                        '#' if self.is_whitespace_at(index.wrapping_sub(1)) => {
                            runs_to_line_end = true;
                            break;
                        }
                        _ => end_pos += 1,
                    }
                }
                if end_pos == self.chars.len() {
                    runs_to_line_end = true;
                }
                let plain = self.slice(self.index(start_pos), self.index(end_pos));
                // Trailing whitespace isn't part of the scalar, YAML strips it.
                let trimmed = plain.trim_end();
                if needs_requote(trimmed) && self.stands_alone(end_pos, flow, indent) {
                    self.output.push('"');
                    self.output.push_str(trimmed);
                    self.output.push('"');
                    self.output.push_str(&plain[trimmed.len()..]);
                } else {
                    self.output.push_str(&plain);
                }
                scalar_column = index - line_start;
                at_start = false;
                in_plain = runs_to_line_end && flow == 0;
                pos = end_pos;
                continue;
            } else if c == ':' && self.is_break(index + c.len_utf8(), flow) {
                // The key/value separator after a plain key
                at_start = true;
                indent = scalar_column;
            }
            self.output.push(c);
            pos += 1;
        }
    }

    /// Whether the plain scalar ending at char [end_pos] stands alone: what follows is the end of
    /// its line, a comment, the key separator or a flow entry, and no more indented continuation
    /// line follows.
    fn stands_alone(&self, end_pos: usize, flow: usize, parent_indent: usize) -> bool {
        let mut index = self.index(end_pos);
        // What immediately follows the scalar.
        match self.byte(index) {
            // The key separator: a key stands alone by definition.
            Some(b':') if self.is_break(index + 1, flow) => return true,
            // The rest of the line: only whitespace and a comment may follow.
            Some(b',' | b']' | b'}') if flow > 0 => return true,
            _ => {}
        }
        while let Some(b) = self.byte(index) {
            match b {
                b' ' | b'\t' | b'\r' => index += 1,
                b'\n' | b'#' => break,
                _ => return false,
            }
        }
        if flow > 0 {
            // In a flow collection a newline doesn't end a plain scalar: a following line with
            // content other than a comment or a flow indicator continues it. Blank and
            // comment-only lines are skipped.
            while index < self.yaml.len() {
                if self.byte(index) == Some(b'\n') {
                    index += 1;
                }
                while matches!(self.byte(index), Some(b' ' | b'\t' | b'\r')) {
                    index += 1;
                }
                match self.byte(index) {
                    None => return true,
                    Some(b'\n') => continue,
                    Some(b'#') => {
                        // A comment-only line: skip to its end and keep looking.
                        while index < self.yaml.len() && self.byte(index) != Some(b'\n') {
                            index += 1;
                        }
                    }
                    Some(b',' | b']' | b'}') => return true,
                    _ => return false,
                }
            }
            return true;
        }
        // A more indented next line continues the scalar, so it doesn't stand alone.
        while index < self.yaml.len() {
            let mut column = 0;
            while matches!(self.byte(index), Some(b' ')) {
                index += 1;
                column += 1;
            }
            match self.byte(index) {
                None | Some(b'\n') => {}
                Some(b'#') => return true,
                _ => return column <= parent_indent,
            }
            // A blank line: keep looking at the one after it.
            while index < self.yaml.len() && self.byte(index) != Some(b'\n') {
                index += 1;
            }
            index += 1;
        }
        true
    }

    /// Copies a quoted scalar's content and closing quote, returning the position after it.
    fn copy_quoted(&mut self, mut pos: usize, quote: char) -> usize {
        while let Some(c) = self.char(pos) {
            self.output.push(c);
            pos += 1;
            if quote == '"' && c == '\\' {
                if let Some(escaped) = self.char(pos) {
                    self.output.push(escaped);
                    pos += 1;
                }
            } else if c == quote {
                if quote == '\'' && self.char(pos) == Some('\'') {
                    self.output.push('\'');
                    pos += 1;
                } else {
                    return pos;
                }
            }
        }
        pos
    }

    /// The byte index after the line break of a block scalar header (`|`, `>-`, `|2 # comment`),
    /// or `None` if it isn't one.
    fn block_header_end(&self, start: usize) -> Option<usize> {
        let mut i = start + 1;
        while matches!(self.byte(i), Some(b'0'..=b'9' | b'+' | b'-')) {
            i += 1;
        }
        while matches!(self.byte(i), Some(b' ' | b'\t')) {
            i += 1;
        }
        if self.byte(i) == Some(b'#') && self.is_whitespace_at(i.wrapping_sub(1)) {
            i = self.yaml[i..]
                .iter()
                .position(|b| *b == b'\n')
                .map(|offset| i + offset)
                .unwrap_or(self.yaml.len());
        }
        match self.byte(i) {
            None => Some(self.yaml.len()),
            Some(b'\n' | b'\r') => self.yaml[i..]
                .iter()
                .position(|b| *b == b'\n')
                .map(|offset| i + offset + 1)
                .or(Some(self.yaml.len())),
            _ => None,
        }
    }

    /// Copies the block scalar header at [pos] and its content lines, more indented than
    /// [parent_indent], unchanged.
    fn copy_block_scalar(&mut self, pos: usize, parent_indent: usize) -> usize {
        let start = self.index(pos);
        let mut i = self.block_header_end(start).unwrap_or(self.yaml.len());
        self.output.push_str(&self.slice(start, i));
        while i < self.yaml.len() {
            let end = self.yaml[i..]
                .iter()
                .position(|b| *b == b'\n')
                .map(|offset| i + offset + 1)
                .unwrap_or(self.yaml.len());
            let line = self.slice(i, end);
            let indented = line.chars().take_while(|c| *c == ' ').count();
            if !line.trim().is_empty() && indented <= parent_indent {
                break;
            }
            self.output.push_str(&line);
            i = end;
        }
        self.pos(i)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn requotes_divergent_plain_scalars() {
        assert_eq!(
            requote_plain_scalars("identifier: Null\n"),
            "identifier: \"Null\"\n"
        );
        assert_eq!(
            requote_plain_scalars("flags: [True, FALSE, plain]\n"),
            "flags: [\"True\", \"FALSE\", plain]\n"
        );
        assert_eq!(
            requote_plain_scalars("values:\n  - .inf\n  - Null\n"),
            "values:\n  - \".inf\"\n  - \"Null\"\n"
        );
    }

    #[test]
    fn leaves_standard_scalars_alone() {
        let yaml = "a: null\nb: ~\nc: true\nd: 5\ne: Nullthing\nf: \"Null\"\ng: Null # comment\nh: Null Thing\n";
        assert_eq!(
            requote_plain_scalars(yaml),
            "a: null\nb: ~\nc: true\nd: 5\ne: Nullthing\nf: \"Null\"\ng: \"Null\" # comment\nh: Null Thing\n"
        );
    }

    #[test]
    fn leaves_comments_and_block_scalars_alone() {
        let yaml = "# Null True .inf\nblock: |\n  Null True .inf\nafter: Null\n";
        assert_eq!(
            requote_plain_scalars(yaml),
            "# Null True .inf\nblock: |\n  Null True .inf\nafter: \"Null\"\n"
        );
    }

    #[test]
    fn requotes_every_divergent_spelling() {
        // Everything the standard parser types but the reference reads as a string.
        let yaml = "a: NULL\nb: 0x1F\nc: 0o17\nd: 0b101\ne: +5\nf: 1.\ng: .5\nh: +.Inf\ni: -.INF\nj: 1.e3\n";
        assert_eq!(
            requote_plain_scalars(yaml),
            "a: \"NULL\"\nb: \"0x1F\"\nc: \"0o17\"\nd: \"0b101\"\ne: \"+5\"\nf: \"1.\"\ng: \".5\"\nh: \"+.Inf\"\ni: \"-.INF\"\nj: \"1.e3\"\n"
        );
    }

    #[test]
    fn leaves_scalars_both_read_as_typed_alone() {
        // Typed by both the standard parser and the reference: nothing to re-quote.
        let yaml =
            "a: 5\nb: -5\nc: 0.5\nd: 1e5\ne: 1E+5\nf: -0.5\ng: 0\nh: 0123\ni: null\nj: true\n";
        assert_eq!(requote_plain_scalars(yaml), yaml);
    }

    #[test]
    fn a_flow_scalar_continues_on_the_next_line() {
        // `[Null\n  continued]` is the single string "Null continued": no re-quoting.
        let yaml = "one_of: [Null\n  continued]\n";
        assert_eq!(requote_plain_scalars(yaml), yaml);
        // A comment line doesn't end the continuation either.
        let yaml = "one_of: [Null\n  # a note\n  continued]\n";
        assert_eq!(requote_plain_scalars(yaml), yaml);
        // A flow indicator or a comment on the scalar's own line does end it.
        assert_eq!(
            requote_plain_scalars("one_of: [Null, x]\n"),
            "one_of: [\"Null\", x]\n"
        );
        assert_eq!(
            requote_plain_scalars("one_of: [Null # note\n]\n"),
            "one_of: [\"Null\" # note\n]\n"
        );
        assert_eq!(
            requote_plain_scalars("one_of: [Null\n]\n"),
            "one_of: [\"Null\"\n]\n"
        );
    }

    #[test]
    fn leaves_plain_continuations_alone() {
        // The continuation line makes the scalar "Null continued", which is not the token.
        let yaml = "a: Null\n  continued\n";
        assert_eq!(requote_plain_scalars(yaml), yaml);
    }
}
