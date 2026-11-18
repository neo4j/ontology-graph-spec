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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.neo4j.spec.graph.codec.schema.SchemaElement;
import org.neo4j.spec.graph.codec.schema.SchemaList;
import org.neo4j.spec.graph.codec.schema.SchemaLiteral;
import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.codec.schema.SchemaNull;
import org.neo4j.spec.graph.model.GraphModel;

/**
 * YAML {@link Format}. Parsing uses jackson-dataformat-yaml (quoted scalars are always strings, plain scalars are
 * literals only when they are booleans or numbers), writing uses {@link YamlWriter} for the graph spec's layout.
 * Models are mapped through the {@link JsonFormat} schema tree.
 */
public final class YamlFormat implements Format {
    private static final YamlFormat DEFAULT = new YamlFormat(
        YAMLMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_LONG_FOR_INTS)
            .build(),
        JsonFormat.getDefault(),
        new YamlPrintOptions(
            2,
            true,
            Set.of(
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
                "extensions.data_source_mappings.*[*].to_node.properties.*")));

    private static final Pattern NUMBER = Pattern.compile("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][-+]?[0-9]+)?");

    private final ObjectMapper yaml;
    private final JsonFormat json;
    private final YamlWriter writer;

    public YamlFormat(ObjectMapper yaml, JsonFormat json, YamlPrintOptions options) {
        this.yaml = yaml;
        this.json = json;
        this.writer = new YamlWriter(options);
    }

    public static YamlFormat getDefault() {
        return DEFAULT;
    }

    @Override
    public String encodeToString(SchemaElement element) {
        return writer.write(element);
    }

    @Override
    public SchemaElement decodeFromString(String string) {
        try (JsonParser parser = yaml.getFactory().createParser(string)) {
            JsonToken token = parser.nextToken();
            return token == null ? new SchemaNull("") : element(parser, token);
        } catch (IOException e) {
            String message = e instanceof JsonProcessingException ? ((JsonProcessingException) e).getOriginalMessage() : e.getMessage();
            throw new IllegalArgumentException(message, e);
        }
    }

    private static SchemaElement element(JsonParser parser, JsonToken token) throws IOException {
        switch (token) {
            case START_ARRAY: {
                List<SchemaElement> content = new ArrayList<>();
                while ((token = parser.nextToken()) != JsonToken.END_ARRAY) content.add(element(parser, token));
                return new SchemaList(content, "");
            }
            case START_OBJECT: {
                Map<String, SchemaElement> content = new LinkedHashMap<>();
                while ((token = parser.nextToken()) != JsonToken.END_OBJECT) {
                    String key = parser.currentName();
                    content.put(key, element(parser, parser.nextToken()));
                }
                return new SchemaMap(content, "");
            }
            case VALUE_NULL: {
                String text = parser.getText();
                return text.equals("null") || text.equals("~") ? new SchemaNull("") : new SchemaLiteral(text, "", true);
            }
            case VALUE_TRUE:
            case VALUE_FALSE: {
                String text = parser.getText();
                return new SchemaLiteral(text, "", !(text.equals("true") || text.equals("false")));
            }
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT: {
                String text = parser.getText();
                return new SchemaLiteral(text, "", !NUMBER.matcher(text).matches());
            }
            default:
                // Quoted scalars are always strings, as are plain scalars that aren't booleans, numbers or null
                return new SchemaLiteral(parser.getText(), "", true);
        }
    }

    @Override
    public SchemaElement encodeToSchema(GraphModel model) {
        return json.encodeToSchema(model);
    }

    @Override
    public GraphModel decodeFromSchema(SchemaElement element) {
        return json.decodeFromSchema(element);
    }
}
