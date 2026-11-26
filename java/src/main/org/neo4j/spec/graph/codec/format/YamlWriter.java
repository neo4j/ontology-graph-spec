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
package org.neo4j.spec.graph.codec.format;

import java.util.Map;
import java.util.regex.Pattern;
import org.neo4j.spec.graph.codec.schema.SchemaElement;
import org.neo4j.spec.graph.codec.schema.SchemaList;
import org.neo4j.spec.graph.codec.schema.SchemaLiteral;
import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.codec.schema.SchemaNull;

/**
 * Pretty prints YAML according to {@link YamlPrintOptions}, matching the Kotlin library's YamlWriter byte for byte.
 */
public final class YamlWriter {
    private static final Pattern WHOLE_DOUBLE = Pattern.compile("-?\\d+\\.0");
    private static final String SPECIAL = ":{}[],&*#?|-<>=!%@`\"'";

    private final YamlPrintOptions options;

    public YamlWriter(YamlPrintOptions options) {
        this.options = options;
    }

    public YamlWriter() {
        this(new YamlPrintOptions());
    }

    public String write(SchemaElement element) {
        StringBuilder builder = new StringBuilder();
        writeElement(element, 0, false, builder);
        return builder.toString().stripTrailing() + "\n";
    }

    private void writeElement(SchemaElement element, int indent, boolean forceInline, StringBuilder builder) {
        boolean inline = forceInline || options.shouldInline(element);
        if (element instanceof SchemaNull) {
            builder.append("null");
        } else if (element instanceof SchemaLiteral) {
            SchemaLiteral literal = (SchemaLiteral) element;
            if (literal.isString()) {
                builder.append(escapeAndQuote(literal.getString(), options.isAlwaysQuoteStrings()));
            } else {
                builder.append(canonicalNumber(literal.getString()));
            }
        } else if (element instanceof SchemaList) {
            SchemaList list = (SchemaList) element;
            if (list.isEmpty()) {
                builder.append("[]");
            } else if (inline) {
                builder.append("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) builder.append(", ");
                    writeElement(list.get(i), indent, true, builder);
                }
                builder.append("]");
            } else {
                for (int i = 0; i < list.size(); i++) {
                    SchemaElement child = list.get(i);
                    if (i > 0) builder.append("\n").append(" ".repeat(indent));
                    builder.append("- ");
                    if (child instanceof SchemaMap && !options.shouldInline(child)) {
                        writeBlockMap((SchemaMap) child, indent + 2, builder);
                    } else if (child instanceof SchemaList && !options.shouldInline(child)) {
                        builder.append("\n").append(" ".repeat(indent + 2));
                        writeElement(child, indent + 2, false, builder);
                    } else {
                        writeElement(child, indent + 2, false, builder);
                    }
                }
            }
        } else if (element instanceof SchemaMap) {
            SchemaMap map = (SchemaMap) element;
            if (map.isEmpty()) {
                builder.append("{}");
            } else if (inline) {
                builder.append("{ ");
                boolean first = true;
                for (Map.Entry<String, SchemaElement> entry : map.entrySet()) {
                    if (!first) builder.append(", ");
                    first = false;
                    builder.append(escapeAndQuote(entry.getKey(), false)).append(": ");
                    writeElement(entry.getValue(), indent, true, builder);
                }
                builder.append(" }");
            } else {
                writeBlockMap(map, indent, builder);
            }
        }
    }

    private void writeBlockMap(SchemaMap map, int indent, StringBuilder builder) {
        boolean first = true;
        for (Map.Entry<String, SchemaElement> entry : map.entrySet()) {
            SchemaElement value = entry.getValue();
            if (!first) builder.append("\n").append(" ".repeat(indent));
            first = false;
            builder.append(escapeAndQuote(entry.getKey(), false)).append(":");
            boolean valueInline = options.shouldInline(value);
            if ((value instanceof SchemaMap || value instanceof SchemaList) && !valueInline) {
                builder.append("\n").append(" ".repeat(indent + options.getIndent()));
                writeElement(value, indent + options.getIndent(), false, builder);
            } else {
                builder.append(" ");
                writeElement(value, indent, false, builder);
            }
        }
    }

    // Kotlin/JVM writes a whole Double as `33.0` and Kotlin/JS as `33`. Drop the `.0` so both write the same YAML.
    private static String canonicalNumber(String value) {
        return WHOLE_DOUBLE.matcher(value).matches() ? value.substring(0, value.length() - 2) : value;
    }

    private static boolean needsQuotes(String value) {
        if (value.isEmpty() || !value.trim().equals(value)) return true;
        for (int i = 0; i < value.length(); i++) {
            if (SPECIAL.indexOf(value.charAt(i)) >= 0) return true;
        }
        if (value.equals("true") || value.equals("false") || value.equals("null")) return true;
        if (value.contains("\n") || value.contains("\t") || value.contains("\r")) return true;
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String escapeAndQuote(String value, boolean forceQuotes) {
        if (!forceQuotes && !needsQuotes(value)) return value;
        String escaped = value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }
}
