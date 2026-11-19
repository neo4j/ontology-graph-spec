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
package org.neo4j.spec.graph.internal.codec;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import org.neo4j.spec.graph.Format;
import org.neo4j.spec.graph.GraphSpecException;
import org.neo4j.spec.graph.internal.yaml.YamlEmitter;

/** Parses JSON and YAML text into document trees, and writes trees as canonical JSON and YAML text. */
public final class TreeCodec {

    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(com.fasterxml.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();
    private static final YAMLFactory YAML = new YAMLFactory();
    private static final JsonNodeFactory F = JsonNodeFactory.instance;

    private TreeCodec() {}

    /**
     * Parses a document.
     *
     * @throws GraphSpecException if the text isn't valid JSON or YAML, or isn't an object
     */
    public static ObjectNode parse(Reader reader, Format format) {
        JsonNode tree;
        try {
            tree = format == Format.JSON ? JSON.readTree(reader) : parseYaml(reader);
        } catch (IOException | RuntimeException e) {
            if (e instanceof GraphSpecException) {
                throw (GraphSpecException) e;
            }
            throw new GraphSpecException("Can't read " + format + " document: " + e.getMessage(), e);
        }
        if (tree == null || !tree.isObject()) {
            throw new GraphSpecException("A graph spec document must be an object");
        }
        return (ObjectNode) tree;
    }

    /** Writes a tree as text in the canonical style of the format, ending with a newline. */
    public static void write(JsonNode tree, Format format, Writer writer) {
        try {
            if (format == Format.JSON) {
                writeJson(tree, writer);
            } else {
                writer.write(YamlEmitter.emit(tree));
            }
        } catch (IOException e) {
            throw new GraphSpecException("Can't write " + format + " document: " + e.getMessage(), e);
        }
    }

    // ---- JSON ------------------------------------------------------------------------------------------------

    private static void writeJson(JsonNode tree, Writer writer) throws IOException {
        try (JsonGenerator g = JSON.getFactory().createGenerator(writer)) {
            g.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
            g.setPrettyPrinter(new Pretty());
            writeNode(g, tree);
        }
        writer.write('\n');
    }

    private static void writeNode(JsonGenerator g, JsonNode n) throws IOException {
        switch (n.getNodeType()) {
            case OBJECT:
                g.writeStartObject();
                for (Map.Entry<String, JsonNode> e : n.properties()) {
                    g.writeFieldName(e.getKey());
                    writeNode(g, e.getValue());
                }
                g.writeEndObject();
                break;
            case ARRAY:
                g.writeStartArray();
                for (JsonNode item : n) {
                    writeNode(g, item);
                }
                g.writeEndArray();
                break;
            case STRING:
                g.writeString(n.textValue());
                break;
            case BOOLEAN:
                g.writeBoolean(n.booleanValue());
                break;
            case NUMBER:
                if (n.isInt()) {
                    g.writeNumber(n.intValue());
                } else if (n.isLong()) {
                    g.writeNumber(n.longValue());
                } else if (n.isBigInteger()) {
                    g.writeNumber(n.bigIntegerValue());
                } else if (n.isBigDecimal()) {
                    g.writeNumber(n.decimalValue());
                } else {
                    g.writeNumber(n.doubleValue());
                }
                break;
            default:
                g.writeNull();
        }
    }

    /** Four-space indentation, {@code "key": value}, one array item per line and {@code {}} / {@code []} when empty. */
    private static final class Pretty implements PrettyPrinter {
        private int depth;

        private void newline(JsonGenerator g) throws IOException {
            g.writeRaw('\n');
            for (int i = 0; i < depth; i++) {
                g.writeRaw("    ");
            }
        }

        @Override
        public void writeRootValueSeparator(JsonGenerator g) throws IOException {
            g.writeRaw(' ');
        }

        @Override
        public void writeStartObject(JsonGenerator g) throws IOException {
            g.writeRaw('{');
            depth++;
        }

        @Override
        public void writeObjectFieldValueSeparator(JsonGenerator g) throws IOException {
            g.writeRaw(": ");
        }

        @Override
        public void writeObjectEntrySeparator(JsonGenerator g) throws IOException {
            g.writeRaw(',');
            newline(g);
        }

        @Override
        public void writeEndObject(JsonGenerator g, int entries) throws IOException {
            depth--;
            if (entries > 0) {
                newline(g);
            }
            g.writeRaw('}');
        }

        @Override
        public void writeStartArray(JsonGenerator g) throws IOException {
            g.writeRaw('[');
            depth++;
        }

        @Override
        public void writeArrayValueSeparator(JsonGenerator g) throws IOException {
            g.writeRaw(',');
            newline(g);
        }

        @Override
        public void writeEndArray(JsonGenerator g, int values) throws IOException {
            depth--;
            if (values > 0) {
                newline(g);
            }
            g.writeRaw(']');
        }

        @Override
        public void beforeArrayValues(JsonGenerator g) throws IOException {
            newline(g);
        }

        @Override
        public void beforeObjectEntries(JsonGenerator g) throws IOException {
            newline(g);
        }
    }

    // ---- YAML ------------------------------------------------------------------------------------------------

    /**
     * Builds a tree from YAML tokens. Plain scalars resolve as in JSON: only {@code null}, {@code ~} and an empty value
     * are null, and only {@code true} and {@code false} are booleans, so a value such as {@code Null} or {@code Yes}
     * stays a string. Quoted scalars are always strings.
     */
    private static JsonNode parseYaml(Reader reader) throws IOException {
        try (JsonParser p = YAML.createParser(reader)) {
            JsonToken t = p.nextToken();
            if (t == null) {
                return F.missingNode();
            }
            JsonNode root = yamlValue(p, t);
            if (p.nextToken() != null) {
                throw new GraphSpecException("Can't read YAML document: more than one document");
            }
            return root;
        }
    }

    private static JsonNode yamlValue(JsonParser p, JsonToken t) throws IOException {
        switch (t) {
            case START_OBJECT: {
                ObjectNode o = F.objectNode();
                while (p.nextToken() == JsonToken.FIELD_NAME) {
                    String key = p.currentName();
                    if (o.has(key)) {
                        throw new GraphSpecException("Can't read YAML document: duplicate key '" + key + "'");
                    }
                    o.set(key, yamlValue(p, p.nextToken()));
                }
                return o;
            }
            case START_ARRAY: {
                ArrayNode a = F.arrayNode();
                for (JsonToken item = p.nextToken(); item != JsonToken.END_ARRAY; item = p.nextToken()) {
                    a.add(yamlValue(p, item));
                }
                return a;
            }
            case VALUE_STRING:
                return F.textNode(p.getText());
            case VALUE_NUMBER_INT:
                switch (p.getNumberType()) {
                    case INT:
                        return F.numberNode(p.getIntValue());
                    case LONG:
                        return F.numberNode(p.getLongValue());
                    default:
                        return F.numberNode(p.getBigIntegerValue());
                }
            case VALUE_NUMBER_FLOAT:
                return F.numberNode(p.getDoubleValue());
            case VALUE_TRUE:
            case VALUE_FALSE:
            case VALUE_NULL: {
                String raw = p.getText();
                if (raw.equals("true")) {
                    return F.booleanNode(true);
                }
                if (raw.equals("false")) {
                    return F.booleanNode(false);
                }
                if (raw.isEmpty() || raw.equals("null") || raw.equals("~")) {
                    return F.nullNode();
                }
                return F.textNode(raw);
            }
            default:
                throw new GraphSpecException("Can't read YAML document: unexpected " + t);
        }
    }
}
