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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.spec.graph.codec.schema.SchemaElement;
import org.neo4j.spec.graph.codec.schema.SchemaList;
import org.neo4j.spec.graph.codec.schema.SchemaLiteral;
import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.codec.schema.SchemaNull;
import org.neo4j.spec.graph.model.GraphModel;

/**
 * JSON {@link Format} backed by Jackson, the equivalent of the Kotlin library's kotlinx-serialization JsonFormat.
 * Hand-written: the rest of the library is generated from the Kotlin sources.
 */
public final class JsonFormat implements Format {
    private static final JsonFormat DEFAULT = new JsonFormat(builder().enable(SerializationFeature.INDENT_OUTPUT).build());

    private final ObjectMapper mapper;

    public JsonFormat(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** The default instance: lenient, ignores unknown keys and pretty prints. */
    public static JsonFormat getDefault() {
        return DEFAULT;
    }

    /**
     * A mapper builder with the settings the format relies on: unknown keys are ignored, integers in custom extension
     * values decode as Long and decimals keep their written precision.
     */
    public static JsonMapper.Builder builder() {
        Separators separators = Separators.createDefaultInstance()
            .withObjectFieldValueSpacing(Separators.Spacing.AFTER)
            .withObjectEmptySeparator("")
            .withArrayEmptySeparator("");
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter(separators)
            .withObjectIndenter(new DefaultIndenter("    ", "\n"))
            .withArrayIndenter(new DefaultIndenter("    ", "\n"));
        return JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.USE_LONG_FOR_INTS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .defaultPrettyPrinter(printer);
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    @Override
    public String encodeToString(SchemaElement element) {
        return write(toJson(element));
    }

    @Override
    public SchemaElement decodeFromString(String string) {
        try {
            return schemaElement(mapper.readTree(string), "");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e.getOriginalMessage(), e);
        }
    }

    @Override
    public SchemaElement encodeToSchema(GraphModel model) {
        return schemaElement(mapper.valueToTree(model), "");
    }

    @Override
    public GraphModel decodeFromSchema(SchemaElement element) {
        try {
            return mapper.treeToValue(toJson(element), GraphModel.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e.getOriginalMessage(), e);
        }
    }

    @Override
    public GraphModel decodeModelFromString(String content) {
        try {
            return mapper.readValue(content, GraphModel.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e.getOriginalMessage(), e);
        }
    }

    @Override
    public String encodeModelToString(GraphModel model) {
        return write(model);
    }

    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static SchemaElement schemaElement(JsonNode node, String path) {
        if (node == null || node.isNull() || node.isMissingNode()) return new SchemaNull(path);
        if (node.isArray()) {
            List<SchemaElement> content = new ArrayList<>();
            for (JsonNode child : node) content.add(schemaElement(child, ""));
            return new SchemaList(content, path);
        }
        if (node.isObject()) {
            Map<String, SchemaElement> content = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                content.put(field.getKey(), schemaElement(field.getValue(), ""));
            }
            return new SchemaMap(content, path);
        }
        if (node.isTextual()) return new SchemaLiteral(node.textValue(), path, true);
        if (node.isBigDecimal()) return new SchemaLiteral(node.decimalValue().toString(), path, false);
        return new SchemaLiteral(node.asText(), path, false);
    }

    private static JsonNode toJson(SchemaElement element) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        if (element instanceof SchemaList) {
            ArrayNode array = factory.arrayNode();
            for (SchemaElement child : ((SchemaList) element).getContent()) array.add(toJson(child));
            return array;
        }
        if (element instanceof SchemaMap) {
            ObjectNode object = factory.objectNode();
            for (Map.Entry<String, SchemaElement> entry : ((SchemaMap) element).getContent().entrySet()) {
                object.set(entry.getKey(), toJson(entry.getValue()));
            }
            return object;
        }
        if (element instanceof SchemaLiteral) {
            SchemaLiteral literal = (SchemaLiteral) element;
            String text = literal.getString();
            if (literal.isString()) return factory.textNode(text);
            if (text.equals("true") || text.equals("false")) return factory.booleanNode(Boolean.parseBoolean(text));
            try {
                if (text.indexOf('.') < 0 && text.indexOf('e') < 0 && text.indexOf('E') < 0) {
                    return factory.numberNode(new BigInteger(text));
                }
                return factory.numberNode(new BigDecimal(text));
            } catch (NumberFormatException e) {
                // Lenient input such as unquoted strings: keep the text as written
                return factory.rawValueNode(new RawValue(text));
            }
        }
        return factory.nullNode();
    }
}
