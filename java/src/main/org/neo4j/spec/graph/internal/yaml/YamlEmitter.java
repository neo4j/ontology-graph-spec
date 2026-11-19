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
package org.neo4j.spec.graph.internal.yaml;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writes a Jackson {@link JsonNode} tree as YAML in the canonical style of the Ontology Graph Specification.
 *
 * <p>The output is compared byte-for-byte by the spec tests, so the style is fixed:
 *
 * <ul>
 *   <li><b>Layout:</b> two-space indentation; block sequences are indented under their key ({@code key:} then
 *       {@code - item} two columns further in); a sequence item that is a mapping starts on the dash line
 *       ({@code - type: "NODE"}) with its remaining keys aligned beneath the first; a sequence inside a sequence is
 *       written {@code - - x}. The output ends with a single newline.
 *   <li><b>Strings</b> (values) are always double-quoted. Backslash, double quote, newline, tab and carriage return
 *       are escaped; other control characters, DEL, NEL, LS, PS and unpaired surrogates as {@code \}{@code uXXXX};
 *       all other characters, including non-ASCII and emoji, are written as-is. Multi-line strings stay on one line
 *       with {@code \n}.
 *   <li><b>Keys</b> are plain when they start with a letter or underscore and continue with letters, digits,
 *       underscores, dots or inner spaces ({@code with space}, {@code yes}, {@code fulltext.analyzer}), and are not
 *       {@code true}, {@code false} or {@code null} in any case. Anything else is double-quoted: hyphens
 *       ({@code "person-name-exists"}), colons, hashes, a leading digit ({@code "123"}), the empty key, etc.
 *   <li><b>Numbers:</b> integers as-is; doubles with an integral value lose the fraction ({@code 40.0} becomes
 *       {@code 40}), others use the shortest round-trip form ({@code -120.5}); NaN and infinity are written
 *       {@code .nan}, {@code .inf} and {@code -.inf}. Booleans are {@code true}/{@code false}, null is {@code null}.
 *   <li><b>Empty containers:</b> inside a block mapping an empty map or list is written on its own line, indented
 *       beneath its key ({@code from_node:} newline {@code {}}); in a sequence it is written inline ({@code - {}}).
 *       Where flow style applies (below) it is written inline: {@code notes: {}}, {@code labels: []}.
 *   <li><b>Flow style</b> ({@code { type: "STRING", dimension: 3 }}, {@code ["name"]}, whole subtree on one line) is
 *       chosen by <i>document path</i>, not by content, so that user data (for example {@code custom}) is always
 *       block style. Flow mappings: property definitions ({@code nodes.*.properties.*},
 *       {@code relationships.*.properties.*}), relationship ends ({@code relationships.*.from}, {@code .to}) and the
 *       property mappings in data source mappings ({@code properties.*} of a mapping, its {@code from_node} and
 *       {@code to_node}). Flow sequences: {@code properties} and {@code labels} of constraints and indexes,
 *       {@code primary_keys}, column {@code supported}, foreign key {@code columns} and {@code references.columns},
 *       and a mapping's {@code key}. Everything else (label {@code implied}/{@code optional}, {@code aliases},
 *       index options, custom data) is block style.
 *   <li><b>Dotted keys:</b> paths are matched on dot-separated segments, so a key containing a dot anywhere on the
 *       path ({@code dotted.id}, {@code order-events.csv}, {@code spatial.cartesian.min}) cannot be matched and
 *       that entry and everything beneath it is block style.
 * </ul>
 */
public final class YamlEmitter {

    private static final int INDENT = 2;

    /** Path segment standing for a sequence index. */
    private static final String ITEM = "[]";

    /** Pattern segment standing for any map key. */
    private static final String ANY = "*";

    private static final List<String[]> FLOW_MAPS = patterns(
            "nodes.*.properties.*",
            "relationships.*.properties.*",
            "relationships.*.from",
            "relationships.*.to",
            "extensions.data_source_mappings.*.[].properties.*",
            "extensions.data_source_mappings.*.[].from_node.properties.*",
            "extensions.data_source_mappings.*.[].to_node.properties.*");

    private static final List<String[]> FLOW_LISTS = patterns(
            "nodes.*.constraints.*.properties",
            "relationships.*.constraints.*.properties",
            "nodes.*.extensions.indexes.*.properties",
            "nodes.*.extensions.indexes.*.labels",
            "relationships.*.extensions.indexes.*.properties",
            "relationships.*.extensions.indexes.*.labels",
            "extensions.data_source_tables.*.primary_keys",
            "extensions.data_source_tables.*.columns.[].supported",
            "extensions.data_source_tables.*.foreign_keys.*.columns",
            "extensions.data_source_tables.*.foreign_keys.*.references.columns",
            "extensions.data_source_mappings.*.[].key");

    private final StringBuilder out = new StringBuilder();

    private YamlEmitter() {}

    /** Returns the canonical YAML text for the document, ending in a newline. */
    public static String emit(JsonNode document) {
        YamlEmitter emitter = new YamlEmitter();
        emitter.root(document);
        return emitter.out.toString();
    }

    /** Writes the canonical YAML text for the document to the writer, ending in a newline. */
    public static void emit(JsonNode document, Writer writer) {
        try {
            writer.write(emit(document));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void root(JsonNode node) {
        if (node.isContainerNode() && node.size() > 0) {
            block(node, 0, new ArrayList<>(), false);
        } else if (node.isContainerNode()) {
            out.append(node.isObject() ? "{}" : "[]").append('\n');
        } else {
            out.append(scalar(node)).append('\n');
        }
    }

    /** Writes a non-empty container as a block; the first line's indentation is skipped when {@code inline}. */
    private void block(JsonNode node, int indent, List<String> path, boolean inline) {
        boolean first = true;
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                if (!(first && inline)) {
                    pad(indent);
                }
                first = false;
                out.append(key(e.getKey())).append(':');
                path.add(e.getKey());
                value(e.getValue(), indent, path);
                path.remove(path.size() - 1);
            }
        } else {
            for (JsonNode item : node) {
                if (!(first && inline)) {
                    pad(indent);
                }
                first = false;
                out.append('-');
                path.add(ITEM);
                if (isFlow(item, path)) {
                    out.append(' ');
                    flow(item);
                    out.append('\n');
                } else if (item.isContainerNode() && item.size() > 0) {
                    out.append(' ');
                    block(item, indent + INDENT, path, true);
                } else if (item.isContainerNode()) {
                    out.append(item.isObject() ? " {}\n" : " []\n");
                } else {
                    out.append(' ').append(scalar(item)).append('\n');
                }
                path.remove(path.size() - 1);
            }
        }
    }

    /** Writes the value after {@code key:} on a block mapping line (the path already includes the key). */
    private void value(JsonNode v, int indent, List<String> path) {
        if (isFlow(v, path)) {
            out.append(' ');
            flow(v);
            out.append('\n');
        } else if (v.isContainerNode()) {
            out.append('\n');
            if (v.size() == 0) {
                pad(indent + INDENT);
                out.append(v.isObject() ? "{}" : "[]").append('\n');
            } else {
                block(v, indent + INDENT, path, false);
            }
        } else {
            out.append(' ').append(scalar(v)).append('\n');
        }
    }

    private void flow(JsonNode node) {
        if (node.isObject()) {
            if (node.size() == 0) {
                out.append("{}");
                return;
            }
            out.append("{ ");
            boolean first = true;
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                if (!first) {
                    out.append(", ");
                }
                first = false;
                out.append(key(e.getKey())).append(": ");
                flow(e.getValue());
            }
            out.append(" }");
        } else if (node.isArray()) {
            out.append('[');
            boolean first = true;
            for (JsonNode item : node) {
                if (!first) {
                    out.append(", ");
                }
                first = false;
                flow(item);
            }
            out.append(']');
        } else {
            out.append(scalar(node));
        }
    }

    /** Whether the node at the path is a container written in flow style. */
    private static boolean isFlow(JsonNode node, List<String> path) {
        if (!node.isContainerNode()) {
            return false;
        }
        for (String segment : path) {
            if (segment.indexOf('.') >= 0) {
                return false;
            }
        }
        return matches(node.isObject() ? FLOW_MAPS : FLOW_LISTS, path);
    }

    private static boolean matches(List<String[]> patterns, List<String> path) {
        for (String[] pattern : patterns) {
            if (pattern.length != path.size()) {
                continue;
            }
            boolean ok = true;
            for (int i = 0; i < pattern.length && ok; i++) {
                ok = pattern[i].equals(ANY) ? !path.get(i).equals(ITEM) : pattern[i].equals(path.get(i));
            }
            if (ok) {
                return true;
            }
        }
        return false;
    }

    private static List<String[]> patterns(String... dotted) {
        List<String[]> result = new ArrayList<>();
        for (String p : dotted) {
            result.add(p.split("\\."));
        }
        return result;
    }

    // ---- scalars ---------------------------------------------------------------------------------------------

    private static String scalar(JsonNode node) {
        if (node.isNull() || node.isMissingNode()) {
            return "null";
        }
        if (node.isBoolean()) {
            return node.booleanValue() ? "true" : "false";
        }
        if (node.isTextual()) {
            return quote(node.textValue());
        }
        if (node.isBigDecimal()) {
            BigDecimal d = node.decimalValue();
            if (d.signum() == 0) {
                return "0";
            }
            BigDecimal s = d.stripTrailingZeros();
            return s.scale() < 0 ? s.toBigInteger().toString() : s.toPlainString();
        }
        if (node.isFloatingPointNumber()) {
            double d = node.isFloat() ? Double.parseDouble(Float.toString(node.floatValue())) : node.doubleValue();
            return number(d);
        }
        if (node.isNumber()) {
            return node.asText();
        }
        return quote(node.asText());
    }

    private static String number(double d) {
        if (Double.isNaN(d)) {
            return ".nan";
        }
        if (Double.isInfinite(d)) {
            return d > 0 ? ".inf" : "-.inf";
        }
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return Long.toString((long) d);
        }
        return Double.toString(d);
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (c < 0x20 || c == 0x7f || c == 0x85 || c == 0x2028 || c == 0x2029) {
                        hex(sb, c);
                    } else if (Character.isHighSurrogate(c)
                            && i + 1 < s.length()
                            && Character.isLowSurrogate(s.charAt(i + 1))) {
                        sb.append(c).append(s.charAt(++i));
                    } else if (Character.isSurrogate(c) || c == 0xfffe || c == 0xffff) {
                        hex(sb, c);
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    private static void hex(StringBuilder sb, char c) {
        sb.append("\\u");
        String h = Integer.toHexString(c).toUpperCase(Locale.ROOT);
        for (int i = h.length(); i < 4; i++) {
            sb.append('0');
        }
        sb.append(h);
    }

    private static String key(String k) {
        return isPlainKey(k) ? k : quote(k);
    }

    private static boolean isPlainKey(String k) {
        if (k.isEmpty()) {
            return false;
        }
        int first = k.codePointAt(0);
        if (!(Character.isLetter(first) || first == '_')) {
            return false;
        }
        for (int i = 0; i < k.length(); ) {
            int cp = k.codePointAt(i);
            i += Character.charCount(cp);
            if (!(Character.isLetterOrDigit(cp) || cp == '_' || cp == '.' || cp == ' ')) {
                return false;
            }
        }
        if (k.charAt(k.length() - 1) == ' ') {
            return false;
        }
        return !Arrays.asList("true", "false", "null").contains(k.toLowerCase(Locale.ROOT));
    }

    private void pad(int n) {
        for (int i = 0; i < n; i++) {
            out.append(' ');
        }
    }
}
