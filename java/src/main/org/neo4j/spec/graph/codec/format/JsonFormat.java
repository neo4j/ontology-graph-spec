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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
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
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.util.RawValue;
import java.io.IOException;
import java.io.StringWriter;
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
 *
 * <p>Only Jackson's streaming parser and tree model are used: models are converted to and from trees by hand, so no
 * reflection or data binding happens at runtime. Mapper settings that only affect data binding (naming strategies,
 * inclusion rules, mix-ins) therefore have no effect on how models are written.
 */
public final class JsonFormat implements Format {
    private static final JsonFormat DEFAULT =
            new JsonFormat(builder().enable(SerializationFeature.INDENT_OUTPUT).build());

    private final ObjectMapper mapper;

    public JsonFormat(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** The default instance: lenient and pretty prints. Unknown keys are always ignored. */
    public static JsonFormat getDefault() {
        return DEFAULT;
    }

    /**
     * A mapper builder with the settings the format relies on: integers in custom extension values decode as Long,
     * decimals keep their written precision and unquoted keys and single quotes are accepted.
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
            return schemaElement(read(string, true), "");
        } catch (IOException e) {
            throw invalid(e);
        }
    }

    @Override
    public SchemaElement encodeToSchema(GraphModel model) {
        return normalised(ModelCodec.encode(model));
    }

    @Override
    public GraphModel decodeFromSchema(SchemaElement element) {
        return ModelCodec.decode(toJson(element), longInts());
    }

    @Override
    public GraphModel decodeModelFromString(String content) {
        try {
            // Decimals keep their written precision in custom extension values
            return ModelCodec.decode(read(content, false), longInts());
        } catch (IOException e) {
            throw invalid(e);
        }
    }

    @Override
    public String encodeModelToString(GraphModel model) {
        return write(ModelCodec.encode(model));
    }

    private boolean longInts() {
        return mapper.isEnabled(DeserializationFeature.USE_LONG_FOR_INTS);
    }

    private static IllegalArgumentException invalid(IOException e) {
        String message = e instanceof JsonProcessingException
                ? ((JsonProcessingException) e).getOriginalMessage()
                : e.getMessage();
        return new IllegalArgumentException(message, e);
    }

    /**
     * Parses a tree with the mapper's parser settings, using the streaming parser directly rather than data binding.
     *
     * @param stripZeros whether decimals drop trailing zeros, as {@link ObjectMapper#readTree(String)} does by default
     */
    private JsonNode read(String content, boolean stripZeros) throws IOException {
        try (JsonParser parser = mapper.createParser(content)) {
            JsonToken token = parser.nextToken();
            if (token == null) return MissingNode.getInstance();
            return read(parser, token, stripZeros);
        }
    }

    private JsonNode read(JsonParser parser, JsonToken token, boolean stripZeros) throws IOException {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        switch (token) {
            case START_OBJECT: {
                ObjectNode object = factory.objectNode();
                while ((token = parser.nextToken()) != JsonToken.END_OBJECT) {
                    String key = parser.currentName();
                    object.set(key, read(parser, parser.nextToken(), stripZeros));
                }
                return object;
            }
            case START_ARRAY: {
                ArrayNode array = factory.arrayNode();
                while ((token = parser.nextToken()) != JsonToken.END_ARRAY) array.add(read(parser, token, stripZeros));
                return array;
            }
            case VALUE_STRING:
                return factory.textNode(parser.getText());
            case VALUE_TRUE:
                return BooleanNode.TRUE;
            case VALUE_FALSE:
                return BooleanNode.FALSE;
            case VALUE_NULL:
                return NullNode.getInstance();
            case VALUE_NUMBER_INT:
                return switch (parser.getNumberType()) {
                    case INT -> IntNode.valueOf(parser.getIntValue());
                    case LONG -> LongNode.valueOf(parser.getLongValue());
                    default -> BigIntegerNode.valueOf(parser.getBigIntegerValue());
                };
            case VALUE_NUMBER_FLOAT:
                if (mapper.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS) && !parser.isNaN()) {
                    BigDecimal decimal = parser.getDecimalValue();
                    if (stripZeros) decimal = decimal.signum() == 0 ? BigDecimal.ZERO : decimal.stripTrailingZeros();
                    return DecimalNode.valueOf(decimal);
                }
                if (parser.getNumberType() == JsonParser.NumberType.FLOAT) {
                    return FloatNode.valueOf(parser.getFloatValue());
                }
                return DoubleNode.valueOf(parser.getDoubleValue());
            case VALUE_EMBEDDED_OBJECT: {
                Object embedded = parser.getEmbeddedObject();
                if (embedded == null) return NullNode.getInstance();
                if (embedded instanceof byte[]) return factory.binaryNode((byte[]) embedded);
                return factory.textNode(embedded.toString());
            }
            default:
                throw new JsonParseException(parser, "Unexpected token " + token);
        }
    }

    /** Writes a tree with the mapper's generator settings, using the streaming generator rather than data binding. */
    private String write(JsonNode value) {
        StringWriter out = new StringWriter();
        try (JsonGenerator generator = mapper.createGenerator(out)) {
            write(generator, value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toString();
    }

    private static void write(JsonGenerator generator, JsonNode value) throws IOException {
        if (value.isObject()) {
            generator.writeStartObject();
            Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                generator.writeFieldName(field.getKey());
                write(generator, field.getValue());
            }
            generator.writeEndObject();
        } else if (value.isArray()) {
            generator.writeStartArray();
            for (JsonNode item : value) write(generator, item);
            generator.writeEndArray();
        } else if (value.isTextual()) {
            generator.writeString(value.textValue());
        } else if (value.isBoolean()) {
            generator.writeBoolean(value.booleanValue());
        } else if (value.isInt() || value.isShort()) {
            generator.writeNumber(value.intValue());
        } else if (value.isLong()) {
            generator.writeNumber(value.longValue());
        } else if (value.isBigInteger()) {
            generator.writeNumber(value.bigIntegerValue());
        } else if (value.isFloat()) {
            generator.writeNumber(value.floatValue());
        } else if (value.isDouble()) {
            generator.writeNumber(value.doubleValue());
        } else if (value.isBigDecimal()) {
            generator.writeNumber(value.decimalValue());
        } else if (value.isBinary()) {
            generator.writeBinary(value.binaryValue());
        } else if (value instanceof POJONode && ((POJONode) value).getPojo() instanceof RawValue) {
            generator.writeRawValue(String.valueOf(((RawValue) ((POJONode) value).getPojo()).rawValue()));
        } else if (value.isNull() || value.isMissingNode()) {
            generator.writeNull();
        } else {
            generator.writeString(value.asText());
        }
    }

    /**
     * The schema of an encoded model. Decimals are written without trailing zeros or exponents, so {@code 10.0} becomes
     * {@code 10}.
     */
    private static SchemaElement normalised(JsonNode node) {
        if (node.isArray()) {
            List<SchemaElement> content = new ArrayList<>();
            for (JsonNode child : node) content.add(normalised(child));
            return new SchemaList(content, "");
        }
        if (node.isObject()) {
            Map<String, SchemaElement> content = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                content.put(field.getKey(), normalised(field.getValue()));
            }
            return new SchemaMap(content, "");
        }
        if (node.isFloatingPointNumber()) {
            BigDecimal decimal = node.isBigDecimal() ? node.decimalValue() : BigDecimal.valueOf(node.doubleValue());
            decimal = decimal.signum() == 0 ? BigDecimal.ZERO : decimal.stripTrailingZeros();
            return new SchemaLiteral(decimal.toPlainString(), "", false);
        }
        return schemaElement(node, "");
    }

    static SchemaElement schemaElement(JsonNode node, String path) {
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
        if (node.isBigDecimal()) return new SchemaLiteral(node.decimalValue().toPlainString(), path, false);
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
            for (Map.Entry<String, SchemaElement> entry :
                    ((SchemaMap) element).getContent().entrySet()) {
                object.set(entry.getKey(), toJson(entry.getValue()));
            }
            return object;
        }
        if (element instanceof SchemaLiteral literal) {
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
