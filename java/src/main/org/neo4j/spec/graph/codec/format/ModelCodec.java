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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.util.RawValue;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.extension.GraphModelExtensions;
import org.neo4j.spec.graph.model.extension.mapping.Mapping;
import org.neo4j.spec.graph.model.extension.mapping.MappingMode;
import org.neo4j.spec.graph.model.extension.mapping.MappingType;
import org.neo4j.spec.graph.model.extension.mapping.NodeMapping;
import org.neo4j.spec.graph.model.extension.mapping.PropertyMapping;
import org.neo4j.spec.graph.model.extension.mapping.QueryMapping;
import org.neo4j.spec.graph.model.extension.mapping.RelationshipMapping;
import org.neo4j.spec.graph.model.extension.mapping.TargetMapping;
import org.neo4j.spec.graph.model.extension.source.ForeignKey;
import org.neo4j.spec.graph.model.extension.source.ForeignKeyReference;
import org.neo4j.spec.graph.model.extension.source.Table;
import org.neo4j.spec.graph.model.extension.source.TableColumn;
import org.neo4j.spec.graph.model.index.FullTextIndexOption;
import org.neo4j.spec.graph.model.index.IndexOption;
import org.neo4j.spec.graph.model.index.PointIndexOption;
import org.neo4j.spec.graph.model.index.VectorIndexOption;
import org.neo4j.spec.graph.model.node.Labels;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.NodeConstraint;
import org.neo4j.spec.graph.model.node.extension.LabelsExtensions;
import org.neo4j.spec.graph.model.node.extension.NodeConstraintExtensions;
import org.neo4j.spec.graph.model.node.extension.NodeDisplay;
import org.neo4j.spec.graph.model.node.extension.NodeExtensions;
import org.neo4j.spec.graph.model.node.extension.NodeIndex;
import org.neo4j.spec.graph.model.property.Neo4jType;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.model.property.extension.PropertyExtensions;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.model.relationship.RelationshipConstraint;
import org.neo4j.spec.graph.model.relationship.RelationshipTarget;
import org.neo4j.spec.graph.model.relationship.extension.RelationshipConstraintExtensions;
import org.neo4j.spec.graph.model.relationship.extension.RelationshipExtensions;
import org.neo4j.spec.graph.model.relationship.extension.RelationshipIndex;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.model.type.IndexType;

/**
 * Converts between {@link GraphModel} and a Jackson tree by hand, so encoding and decoding never rely on reflection.
 *
 * <p>Encoding writes keys in the specification's order and leaves out every value that equals its default. Decoding
 * ignores unknown keys, treats explicit nulls as absent and coerces scalars leniently (numbers and booleans are accepted
 * as strings and vice versa).
 */
final class ModelCodec {
    private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    private ModelCodec() {}

    // ---------------------------------------------------------------------------------------------------------------
    // Encoding
    // ---------------------------------------------------------------------------------------------------------------

    static ObjectNode encode(GraphModel model) {
        ObjectNode node = FACTORY.objectNode();
        node.set("schema", text(model.getSchema()));
        put(node, "version", model.getVersion(), null, ModelCodec::text);
        put(node, "id", model.getId(), null, ModelCodec::text);
        put(node, "name", model.getName(), "", ModelCodec::text);
        put(node, "description", model.getDescription(), "", ModelCodec::text);
        put(node, "nodes", model.getNodes(), Map.of(), map(ModelCodec::encodeNode));
        put(node, "relationships", model.getRelationships(), Map.of(), map(ModelCodec::encodeRelationship));
        put(
                node,
                "extensions",
                model.getExtensions(),
                GraphModelExtensions.builder().build(),
                ModelCodec::encodeGraphModelExtensions);
        return node;
    }

    private static JsonNode encodeNode(Node value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "label", value.getLabel(), null, ModelCodec::text);
        put(node, "labels", value.getLabels(), Labels.builder().build(), ModelCodec::encodeLabels);
        put(node, "properties", value.getProperties(), Map.of(), map(ModelCodec::encodeProperty));
        put(node, "constraints", value.getConstraints(), Map.of(), map(ModelCodec::encodeNodeConstraint));
        put(
                node,
                "extensions",
                value.getExtensions(),
                NodeExtensions.builder().build(),
                ModelCodec::encodeNodeExtensions);
        put(node, "description", value.getDescription(), "", ModelCodec::text);
        put(node, "aliases", value.getAliases(), List.of(), ModelCodec::strings);
        put(node, "reference", value.getReference(), "", ModelCodec::text);
        return node;
    }

    private static JsonNode encodeLabels(Labels value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "identifier", value.getIdentifier(), null, ModelCodec::text);
        put(node, "implied", value.getImplied(), List.of(), ModelCodec::strings);
        put(node, "optional", value.getOptional(), List.of(), ModelCodec::strings);
        put(
                node,
                "extensions",
                value.getExtensions(),
                LabelsExtensions.builder().build(),
                extensions -> encodeCustomOnly(extensions.getCustom()));
        return node;
    }

    private static JsonNode encodeProperty(Property value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "type", value.getType(), Neo4jType.ANY, type -> text(type.value()));
        put(node, "dimension", value.getDimension(), null, FACTORY::numberNode);
        put(node, "must_exist", value.getMustExist(), null, FACTORY::booleanNode);
        put(node, "unique", value.getUnique(), null, FACTORY::booleanNode);
        put(node, "key", value.getKey(), null, FACTORY::booleanNode);
        put(
                node,
                "extensions",
                value.getExtensions(),
                PropertyExtensions.builder().build(),
                extensions -> encodeCustomOnly(extensions.getCustom()));
        put(node, "name", value.getName(), null, ModelCodec::text);
        put(node, "description", value.getDescription(), "", ModelCodec::text);
        put(node, "aliases", value.getAliases(), List.of(), ModelCodec::strings);
        put(node, "reference", value.getReference(), "", ModelCodec::text);
        put(node, "pattern", value.getPattern(), "", ModelCodec::text);
        put(node, "one_of", value.getOneOf(), List.of(), ModelCodec::strings);
        return node;
    }

    private static JsonNode encodeNodeConstraint(NodeConstraint value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "type", value.getType(), null, type -> text(type.name()));
        put(node, "properties", value.getProperties(), null, ModelCodec::strings);
        put(
                node,
                "extensions",
                value.getExtensions(),
                NodeConstraintExtensions.builder().build(),
                extensions -> encodeCustomOnly(extensions.getCustom()));
        put(node, "name", value.getName(), null, ModelCodec::text);
        return node;
    }

    private static JsonNode encodeNodeExtensions(NodeExtensions value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "indexes", value.getIndexes(), Map.of(), map(ModelCodec::encodeNodeIndex));
        put(node, "display", value.getDisplay(), null, ModelCodec::encodeNodeDisplay);
        put(node, "custom", value.getCustom(), Map.of(), ModelCodec::custom);
        return node;
    }

    private static JsonNode encodeNodeIndex(NodeIndex value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "type", value.getType(), null, type -> text(type.name()));
        put(node, "labels", value.getLabels(), null, ModelCodec::strings);
        put(node, "properties", value.getProperties(), null, ModelCodec::strings);
        put(node, "options", value.getOptions(), null, ModelCodec::encodeIndexOption);
        put(node, "custom", value.getCustom(), Map.of(), ModelCodec::custom);
        put(node, "name", value.getName(), null, ModelCodec::text);
        return node;
    }

    private static JsonNode encodeNodeDisplay(NodeDisplay value) {
        ObjectNode node = FACTORY.objectNode();
        node.put("x", value.getX());
        node.put("y", value.getY());
        put(node, "colour", value.getColour(), null, ModelCodec::text);
        put(node, "custom", value.getCustom(), Map.of(), ModelCodec::custom);
        return node;
    }

    private static JsonNode encodeIndexOption(IndexOption value) {
        ObjectNode node = FACTORY.objectNode();
        node.put("type", value.getType().name());
        if (value instanceof FullTextIndexOption) {
            FullTextIndexOption option = (FullTextIndexOption) value;
            FullTextIndexOption defaults = FullTextIndexOption.builder().build();
            put(
                    node,
                    "fulltext.default_analyzer",
                    option.getDefaultAnalyzer(),
                    defaults.getDefaultAnalyzer(),
                    ModelCodec::text);
            put(node, "fulltext.analyzer", option.getAnalyzer(), defaults.getAnalyzer(), ModelCodec::text);
            put(
                    node,
                    "fulltext.eventually_consistent",
                    option.getEventuallyConsistent(),
                    defaults.getEventuallyConsistent(),
                    FACTORY::booleanNode);
            put(
                    node,
                    "fulltext.eventually_consistent_apply_parallelism",
                    option.getEventuallyConsistentApplyParallelism(),
                    defaults.getEventuallyConsistentApplyParallelism(),
                    FACTORY::numberNode);
            put(
                    node,
                    "fulltext.eventually_consistent_refresh_interval",
                    option.getEventuallyConsistentRefreshInterval(),
                    defaults.getEventuallyConsistentRefreshInterval(),
                    ModelCodec::text);
            put(
                    node,
                    "fulltext.eventually_consistent_refresh_parallelism",
                    option.getEventuallyConsistentRefreshParallelism(),
                    defaults.getEventuallyConsistentRefreshParallelism(),
                    FACTORY::numberNode);
            put(
                    node,
                    "fulltext.eventually_consistent_index_update_queue_max_length",
                    option.getEventuallyConsistentUpdateQueueMaxLength(),
                    defaults.getEventuallyConsistentUpdateQueueMaxLength(),
                    FACTORY::numberNode);
        } else if (value instanceof PointIndexOption) {
            PointIndexOption option = (PointIndexOption) value;
            PointIndexOption defaults = PointIndexOption.builder().build();
            put(
                    node,
                    "spatial.cartesian.min",
                    option.getCartesianMin(),
                    defaults.getCartesianMin(),
                    ModelCodec::doubles);
            put(
                    node,
                    "spatial.cartesian.max",
                    option.getCartesianMax(),
                    defaults.getCartesianMax(),
                    ModelCodec::doubles);
            put(
                    node,
                    "spatial.cartesian-3d.min",
                    option.getCartesian3DMin(),
                    defaults.getCartesian3DMin(),
                    ModelCodec::doubles);
            put(
                    node,
                    "spatial.cartesian-3d.max",
                    option.getCartesian3DMax(),
                    defaults.getCartesian3DMax(),
                    ModelCodec::doubles);
            put(node, "spatial.wgs-84.min", option.getWgs84Min(), defaults.getWgs84Min(), ModelCodec::doubles);
            put(node, "spatial.wgs-84.max", option.getWgs84Max(), defaults.getWgs84Max(), ModelCodec::doubles);
            put(node, "spatial.wgs-84-3d.min", option.getWgs843DMin(), defaults.getWgs843DMin(), ModelCodec::doubles);
            put(node, "spatial.wgs-84-3d.max", option.getWgs843DMax(), defaults.getWgs843DMax(), ModelCodec::doubles);
        } else if (value instanceof VectorIndexOption) {
            VectorIndexOption option = (VectorIndexOption) value;
            VectorIndexOption defaults = VectorIndexOption.builder().build();
            put(node, "vector.dimensions", option.getDimensions(), defaults.getDimensions(), FACTORY::numberNode);
            put(
                    node,
                    "vector.similarity_function",
                    option.getSimilarityFunction(),
                    defaults.getSimilarityFunction(),
                    ModelCodec::text);
            put(
                    node,
                    "vector.default_search_expansion_factor",
                    option.getDefaultSearchExpansionFactor(),
                    defaults.getDefaultSearchExpansionFactor(),
                    FACTORY::numberNode);
            put(
                    node,
                    "vector.quantization.enabled",
                    option.getQuantizationEnabled(),
                    defaults.getQuantizationEnabled(),
                    FACTORY::booleanNode);
            put(
                    node,
                    "vector.quantization.type",
                    option.getQuantizationType(),
                    defaults.getQuantizationType(),
                    ModelCodec::text);
            put(node, "vector.hnsw.m", option.getHnswM(), defaults.getHnswM(), FACTORY::numberNode);
            put(
                    node,
                    "vector.hnsw.ef_construction",
                    option.getHnswEfConstruction(),
                    defaults.getHnswEfConstruction(),
                    FACTORY::numberNode);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported index option " + value.getClass().getName());
        }
        return node;
    }

    private static JsonNode encodeRelationship(Relationship value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "type", value.getType(), null, ModelCodec::text);
        put(node, "from", value.getFrom(), null, ModelCodec::encodeRelationshipTarget);
        put(node, "to", value.getTo(), null, ModelCodec::encodeRelationshipTarget);
        put(node, "properties", value.getProperties(), Map.of(), map(ModelCodec::encodeProperty));
        put(node, "constraints", value.getConstraints(), Map.of(), map(ModelCodec::encodeRelationshipConstraint));
        put(
                node,
                "extensions",
                value.getExtensions(),
                RelationshipExtensions.builder().build(),
                ModelCodec::encodeRelationshipExtensions);
        put(node, "description", value.getDescription(), "", ModelCodec::text);
        put(node, "aliases", value.getAliases(), List.of(), ModelCodec::strings);
        put(node, "reference", value.getReference(), "", ModelCodec::text);
        return node;
    }

    private static JsonNode encodeRelationshipTarget(RelationshipTarget value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "node", value.getNode(), "", ModelCodec::text);
        put(node, "label", value.getLabel(), "", ModelCodec::text);
        put(node, "count", value.getCount(), null, FACTORY::numberNode);
        put(node, "min_count", value.getMinCount(), null, FACTORY::numberNode);
        put(node, "max_count", value.getMaxCount(), null, FACTORY::numberNode);
        return node;
    }

    private static JsonNode encodeRelationshipConstraint(RelationshipConstraint value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "type", value.getType(), null, type -> text(type.name()));
        put(node, "properties", value.getProperties(), null, ModelCodec::strings);
        put(
                node,
                "extensions",
                value.getExtensions(),
                RelationshipConstraintExtensions.builder().build(),
                extensions -> encodeCustomOnly(extensions.getCustom()));
        put(node, "name", value.getName(), null, ModelCodec::text);
        return node;
    }

    private static JsonNode encodeRelationshipExtensions(RelationshipExtensions value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "indexes", value.getIndexes(), Map.of(), map(ModelCodec::encodeRelationshipIndex));
        put(node, "custom", value.getCustom(), Map.of(), ModelCodec::custom);
        return node;
    }

    private static JsonNode encodeRelationshipIndex(RelationshipIndex value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "type", value.getType(), null, type -> text(type.name()));
        put(node, "properties", value.getProperties(), null, ModelCodec::strings);
        put(node, "options", value.getOptions(), null, ModelCodec::encodeIndexOption);
        put(node, "custom", value.getCustom(), Map.of(), ModelCodec::custom);
        put(node, "name", value.getName(), null, ModelCodec::text);
        return node;
    }

    private static JsonNode encodeGraphModelExtensions(GraphModelExtensions value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "data_source_tables", value.getDataSourceTables(), Map.of(), map(ModelCodec::encodeTable));
        put(
                node,
                "data_source_mappings",
                value.getDataSourceMappings(),
                Map.of(),
                map(mappings -> list(mappings, ModelCodec::encodeMapping)));
        put(node, "custom", value.getCustom(), Map.of(), ModelCodec::custom);
        return node;
    }

    private static JsonNode encodeTable(Table value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "source", value.getSource(), null, ModelCodec::text);
        put(
                node,
                "columns",
                value.getColumns(),
                List.of(),
                (List<TableColumn> columns) -> list(columns, ModelCodec::encodeTableColumn));
        put(node, "primary_keys", value.getPrimaryKeys(), List.of(), ModelCodec::strings);
        put(node, "foreign_keys", value.getForeignKeys(), Map.of(), map(ModelCodec::encodeForeignKey));
        put(node, "custom", value.getCustom(), Map.of(), ModelCodec::custom);
        return node;
    }

    private static JsonNode encodeTableColumn(TableColumn value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "name", value.getName(), "", ModelCodec::text);
        put(node, "type", value.getType(), "", ModelCodec::text);
        put(node, "size", value.getSize(), -1, FACTORY::numberNode);
        put(node, "suggested", value.getSuggested(), Neo4jType.ANY, type -> text(type.value()));
        put(
                node,
                "supported",
                value.getSupported(),
                List.of(),
                (List<Neo4jType> types) -> list(types, type -> type == null ? FACTORY.nullNode() : text(type.value())));
        put(node, "nullable", value.getNullable(), true, FACTORY::booleanNode);
        put(node, "dimension", value.getDimension(), null, FACTORY::numberNode);
        put(node, "custom", value.getCustom(), Map.of(), ModelCodec::custom);
        return node;
    }

    private static JsonNode encodeForeignKey(ForeignKey value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "columns", value.getColumns(), null, ModelCodec::strings);
        put(node, "references", value.getReferences(), null, ModelCodec::encodeForeignKeyReference);
        put(node, "custom", value.getCustom(), Map.of(), ModelCodec::custom);
        return node;
    }

    private static JsonNode encodeForeignKeyReference(ForeignKeyReference value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "table", value.getTable(), null, ModelCodec::text);
        put(node, "columns", value.getColumns(), List.of(), ModelCodec::strings);
        put(node, "custom", value.getCustom(), Map.of(), ModelCodec::custom);
        return node;
    }

    private static JsonNode encodeMapping(Mapping value) {
        ObjectNode node = FACTORY.objectNode();
        if (value instanceof NodeMapping) {
            NodeMapping mapping = (NodeMapping) value;
            node.put("type", MappingType.NODE);
            put(node, "node", mapping.getNode(), null, ModelCodec::text);
            put(node, "table", mapping.getTable(), null, ModelCodec::text);
            put(node, "properties", mapping.getProperties(), null, map(ModelCodec::encodePropertyMapping));
            put(node, "mode", mapping.getMode(), MappingMode.MERGE, mode -> text(mode.name()));
            put(node, "match_label", mapping.getMatchLabel(), null, ModelCodec::text);
            put(node, "key", mapping.getKey(), List.of(), ModelCodec::strings);
            put(node, "depends_on", mapping.getDependsOn(), "", ModelCodec::text);
            put(node, "id", mapping.getId(), "", ModelCodec::text);
        } else if (value instanceof QueryMapping) {
            QueryMapping mapping = (QueryMapping) value;
            node.put("type", MappingType.QUERY);
            put(node, "table", mapping.getTable(), null, ModelCodec::text);
            put(node, "query", mapping.getQuery(), null, ModelCodec::text);
            put(node, "depends_on", mapping.getDependsOn(), "", ModelCodec::text);
            put(node, "id", mapping.getId(), "", ModelCodec::text);
        } else if (value instanceof RelationshipMapping) {
            RelationshipMapping mapping = (RelationshipMapping) value;
            node.put("type", MappingType.RELATIONSHIP);
            put(node, "relationship", mapping.getRelationship(), null, ModelCodec::text);
            put(node, "table", mapping.getTable(), null, ModelCodec::text);
            put(node, "from_node", mapping.getFromNode(), null, ModelCodec::encodeTargetMapping);
            put(node, "to_node", mapping.getToNode(), null, ModelCodec::encodeTargetMapping);
            put(node, "match_label", mapping.getMatchLabel(), null, ModelCodec::text);
            put(node, "properties", mapping.getProperties(), Map.of(), map(ModelCodec::encodePropertyMapping));
            put(node, "mode", mapping.getMode(), MappingMode.MERGE, mode -> text(mode.name()));
            put(node, "key", mapping.getKey(), List.of(), ModelCodec::strings);
            put(node, "depends_on", mapping.getDependsOn(), "", ModelCodec::text);
            put(node, "id", mapping.getId(), "", ModelCodec::text);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported mapping " + value.getClass().getName());
        }
        return node;
    }

    private static JsonNode encodeTargetMapping(TargetMapping value) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "label", value.getLabel(), "", ModelCodec::text);
        put(node, "properties", value.getProperties(), Map.of(), map(ModelCodec::encodePropertyMapping));
        return node;
    }

    private static JsonNode encodePropertyMapping(PropertyMapping value) {
        ObjectNode node = FACTORY.objectNode();
        node.set("column", text(value.getColumn()));
        return node;
    }

    private static JsonNode encodeCustomOnly(Map<String, Object> custom) {
        ObjectNode node = FACTORY.objectNode();
        put(node, "custom", custom, Map.of(), ModelCodec::custom);
        return node;
    }

    /** Sets {@code key} unless {@code value} equals its default; a {@code null} that isn't the default is written. */
    private static <T> void put(
            ObjectNode node, String key, T value, T defaultValue, Function<? super T, ? extends JsonNode> encoder) {
        if (Objects.equals(defaultValue, value)) return;
        node.set(key, value == null ? FACTORY.nullNode() : encoder.apply(value));
    }

    private static JsonNode text(String value) {
        return value == null ? FACTORY.nullNode() : FACTORY.textNode(value);
    }

    private static <T> Function<Map<String, T>, JsonNode> map(Function<? super T, ? extends JsonNode> encoder) {
        return values -> {
            ObjectNode node = FACTORY.objectNode();
            values.forEach((key, value) -> node.set(key, value == null ? FACTORY.nullNode() : encoder.apply(value)));
            return node;
        };
    }

    private static <T> ArrayNode list(List<T> values, Function<? super T, ? extends JsonNode> encoder) {
        ArrayNode array = FACTORY.arrayNode();
        for (T value : values) array.add(value == null ? FACTORY.nullNode() : encoder.apply(value));
        return array;
    }

    private static ArrayNode strings(List<String> values) {
        return list(values, ModelCodec::text);
    }

    private static ArrayNode doubles(List<Double> values) {
        return list(values, FACTORY::numberNode);
    }

    /** Encodes a custom extension value: maps, collections, arrays, strings, numbers, booleans and enums. */
    static JsonNode custom(Object value) {
        if (value == null) return FACTORY.nullNode();
        if (value instanceof JsonNode) return (JsonNode) value;
        if (value instanceof String) return FACTORY.textNode((String) value);
        if (value instanceof Character) return FACTORY.textNode(value.toString());
        if (value instanceof Boolean) return FACTORY.booleanNode((Boolean) value);
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return FACTORY.numberNode(((Number) value).intValue());
        }
        if (value instanceof Long) return FACTORY.numberNode((Long) value);
        if (value instanceof BigInteger) return FACTORY.numberNode((BigInteger) value);
        if (value instanceof Double) return FACTORY.numberNode((Double) value);
        if (value instanceof Float) return FACTORY.numberNode((Float) value);
        // Bypasses the factory, which may strip trailing zeros
        if (value instanceof BigDecimal) return DecimalNode.valueOf((BigDecimal) value);
        if (value instanceof Neo4jType) return FACTORY.textNode(((Neo4jType) value).value());
        if (value instanceof Enum) return FACTORY.textNode(((Enum<?>) value).name());
        if (value instanceof Map) {
            ObjectNode node = FACTORY.objectNode();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                node.set(String.valueOf(entry.getKey()), custom(entry.getValue()));
            }
            return node;
        }
        if (value instanceof Iterable) {
            ArrayNode array = FACTORY.arrayNode();
            for (Object item : (Iterable<?>) value) array.add(custom(item));
            return array;
        }
        if (value instanceof byte[]) return FACTORY.binaryNode((byte[]) value);
        if (value.getClass().isArray()) {
            ArrayNode array = FACTORY.arrayNode();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) array.add(custom(Array.get(value, i)));
            return array;
        }
        throw new IllegalArgumentException(
                "Unsupported custom extension value of type " + value.getClass().getName());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Decoding
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Decodes a model.
     *
     * @param longInts whether integers in custom extension values decode as {@link Long} rather than the smallest of
     *     {@link Integer}, {@link Long} and {@link BigInteger} that fits
     */
    static GraphModel decode(JsonNode json, boolean longInts) {
        return new Decoder(longInts).graphModel(json);
    }

    private static final class Decoder {
        private final boolean longInts;

        Decoder(boolean longInts) {
            this.longInts = longInts;
        }

        GraphModel graphModel(JsonNode json) {
            Fields fields = fields(json, "");
            if (fields == null) throw new IllegalArgumentException("Expected an object but found null");
            GraphModel.Builder builder =
                    GraphModel.builder(fields.requiredString("version"), fields.requiredString("id"));
            fields.string("schema", builder::schema);
            fields.string("name", builder::name);
            fields.string("description", builder::description);
            fields.map("nodes", this::node, builder::nodes);
            fields.map("relationships", this::relationship, builder::relationships);
            fields.object("extensions", this::graphModelExtensions, builder::extensions);
            return builder.build();
        }

        Node node(Fields fields) {
            Node.Builder builder = Node.builder();
            fields.string("label", builder::label);
            fields.object("labels", this::labels, builder::labels);
            fields.map("properties", this::property, builder::properties);
            fields.map("constraints", this::nodeConstraint, builder::constraints);
            fields.object("extensions", this::nodeExtensions, builder::extensions);
            fields.string("description", builder::description);
            fields.strings("aliases", builder::aliases);
            fields.string("reference", builder::reference);
            return builder.build();
        }

        Labels labels(Fields fields) {
            Labels.Builder builder = Labels.builder();
            fields.string("identifier", builder::identifier);
            fields.strings("implied", builder::implied);
            fields.strings("optional", builder::optional);
            fields.object(
                    "extensions",
                    extensions -> {
                        LabelsExtensions.Builder extensionsBuilder = LabelsExtensions.builder();
                        extensions.custom(extensionsBuilder::custom);
                        return extensionsBuilder.build();
                    },
                    builder::extensions);
            return builder.build();
        }

        Property property(Fields fields) {
            Property.Builder builder = Property.builder();
            fields.neo4jType("type", builder::type);
            fields.integer("dimension", builder::dimension);
            fields.bool("must_exist", builder::mustExist);
            fields.bool("unique", builder::unique);
            fields.bool("key", builder::key);
            fields.object(
                    "extensions",
                    extensions -> {
                        PropertyExtensions.Builder extensionsBuilder = PropertyExtensions.builder();
                        extensions.custom(extensionsBuilder::custom);
                        return extensionsBuilder.build();
                    },
                    builder::extensions);
            fields.string("name", builder::name);
            fields.string("description", builder::description);
            fields.strings("aliases", builder::aliases);
            fields.string("reference", builder::reference);
            fields.string("pattern", builder::pattern);
            fields.strings("one_of", builder::oneOf);
            return builder.build();
        }

        NodeConstraint nodeConstraint(Fields fields) {
            NodeConstraint.Builder builder = NodeConstraint.builder(
                    fields.requiredEnum("type", ConstraintType.class), fields.requiredStrings("properties"));
            fields.object(
                    "extensions",
                    extensions -> {
                        NodeConstraintExtensions.Builder extensionsBuilder = NodeConstraintExtensions.builder();
                        extensions.custom(extensionsBuilder::custom);
                        return extensionsBuilder.build();
                    },
                    builder::extensions);
            fields.string("name", builder::name);
            return builder.build();
        }

        NodeExtensions nodeExtensions(Fields fields) {
            NodeExtensions.Builder builder = NodeExtensions.builder();
            fields.map("indexes", this::nodeIndex, builder::indexes);
            fields.object("display", this::nodeDisplay, builder::display);
            fields.custom(builder::custom);
            return builder.build();
        }

        NodeIndex nodeIndex(Fields fields) {
            NodeIndex.Builder builder = NodeIndex.builder(
                    fields.requiredEnum("type", IndexType.class),
                    fields.requiredStrings("labels"),
                    fields.requiredStrings("properties"));
            fields.object("options", this::indexOption, builder::options);
            fields.custom(builder::custom);
            fields.string("name", builder::name);
            return builder.build();
        }

        NodeDisplay nodeDisplay(Fields fields) {
            NodeDisplay.Builder builder = NodeDisplay.builder(fields.requiredDouble("x"), fields.requiredDouble("y"));
            fields.string("colour", builder::colour);
            fields.custom(builder::custom);
            return builder.build();
        }

        IndexOption indexOption(Fields fields) {
            String type = fields.requiredString("type");
            switch (type) {
                case "FULLTEXT": {
                    FullTextIndexOption.Builder builder = FullTextIndexOption.builder();
                    fields.string("fulltext.default_analyzer", builder::defaultAnalyzer);
                    fields.string("fulltext.analyzer", builder::analyzer);
                    fields.bool("fulltext.eventually_consistent", builder::eventuallyConsistent);
                    fields.integer(
                            "fulltext.eventually_consistent_apply_parallelism",
                            builder::eventuallyConsistentApplyParallelism);
                    fields.string(
                            "fulltext.eventually_consistent_refresh_interval",
                            builder::eventuallyConsistentRefreshInterval);
                    fields.integer(
                            "fulltext.eventually_consistent_refresh_parallelism",
                            builder::eventuallyConsistentRefreshParallelism);
                    fields.integer(
                            "fulltext.eventually_consistent_index_update_queue_max_length",
                            builder::eventuallyConsistentUpdateQueueMaxLength);
                    return builder.build();
                }
                case "POINT": {
                    PointIndexOption.Builder builder = PointIndexOption.builder();
                    fields.doubles("spatial.cartesian.min", builder::cartesianMin);
                    fields.doubles("spatial.cartesian.max", builder::cartesianMax);
                    fields.doubles("spatial.cartesian-3d.min", builder::cartesian3DMin);
                    fields.doubles("spatial.cartesian-3d.max", builder::cartesian3DMax);
                    fields.doubles("spatial.wgs-84.min", builder::wgs84Min);
                    fields.doubles("spatial.wgs-84.max", builder::wgs84Max);
                    fields.doubles("spatial.wgs-84-3d.min", builder::wgs843DMin);
                    fields.doubles("spatial.wgs-84-3d.max", builder::wgs843DMax);
                    return builder.build();
                }
                case "VECTOR": {
                    VectorIndexOption.Builder builder = VectorIndexOption.builder();
                    fields.integer("vector.dimensions", builder::dimensions);
                    fields.string("vector.similarity_function", builder::similarityFunction);
                    fields.floatValue("vector.default_search_expansion_factor", builder::defaultSearchExpansionFactor);
                    fields.bool("vector.quantization.enabled", builder::quantizationEnabled);
                    fields.string("vector.quantization.type", builder::quantizationType);
                    fields.integer("vector.hnsw.m", builder::hnswM);
                    fields.integer("vector.hnsw.ef_construction", builder::hnswEfConstruction);
                    return builder.build();
                }
                default:
                    throw fields.error("type", "Unknown index option type '" + type + "'");
            }
        }

        Relationship relationship(Fields fields) {
            Relationship.Builder builder = Relationship.builder(
                    fields.requiredString("type"),
                    fields.requiredObject("from", this::relationshipTarget),
                    fields.requiredObject("to", this::relationshipTarget));
            fields.map("properties", this::property, builder::properties);
            fields.map("constraints", this::relationshipConstraint, builder::constraints);
            fields.object("extensions", this::relationshipExtensions, builder::extensions);
            fields.string("description", builder::description);
            fields.strings("aliases", builder::aliases);
            fields.string("reference", builder::reference);
            return builder.build();
        }

        RelationshipTarget relationshipTarget(Fields fields) {
            RelationshipTarget.Builder builder = RelationshipTarget.builder();
            fields.string("node", builder::node);
            fields.string("label", builder::label);
            fields.integer("count", builder::count);
            fields.integer("min_count", builder::minCount);
            fields.integer("max_count", builder::maxCount);
            return builder.build();
        }

        RelationshipConstraint relationshipConstraint(Fields fields) {
            RelationshipConstraint.Builder builder = RelationshipConstraint.builder(
                    fields.requiredEnum("type", ConstraintType.class), fields.requiredStrings("properties"));
            fields.object(
                    "extensions",
                    extensions -> {
                        RelationshipConstraintExtensions.Builder extensionsBuilder =
                                RelationshipConstraintExtensions.builder();
                        extensions.custom(extensionsBuilder::custom);
                        return extensionsBuilder.build();
                    },
                    builder::extensions);
            fields.string("name", builder::name);
            return builder.build();
        }

        RelationshipExtensions relationshipExtensions(Fields fields) {
            RelationshipExtensions.Builder builder = RelationshipExtensions.builder();
            fields.map("indexes", this::relationshipIndex, builder::indexes);
            fields.custom(builder::custom);
            return builder.build();
        }

        RelationshipIndex relationshipIndex(Fields fields) {
            RelationshipIndex.Builder builder = RelationshipIndex.builder(
                    fields.requiredEnum("type", IndexType.class), fields.requiredStrings("properties"));
            fields.object("options", this::indexOption, builder::options);
            fields.custom(builder::custom);
            fields.string("name", builder::name);
            return builder.build();
        }

        GraphModelExtensions graphModelExtensions(Fields fields) {
            GraphModelExtensions.Builder builder = GraphModelExtensions.builder();
            fields.map("data_source_tables", this::table, builder::dataSourceTables);
            fields.mapOf(
                    "data_source_mappings",
                    (value, path) -> list(value, path, this::mapping),
                    builder::dataSourceMappings);
            fields.custom(builder::custom);
            return builder.build();
        }

        Table table(Fields fields) {
            Table.Builder builder = Table.builder(fields.requiredString("source"));
            fields.list("columns", this::tableColumn, builder::columns);
            fields.strings("primary_keys", builder::primaryKeys);
            fields.map("foreign_keys", this::foreignKey, builder::foreignKeys);
            fields.custom(builder::custom);
            return builder.build();
        }

        TableColumn tableColumn(Fields fields) {
            TableColumn.Builder builder = TableColumn.builder();
            fields.string("name", builder::name);
            fields.string("type", builder::type);
            fields.integer("size", builder::size);
            fields.neo4jType("suggested", builder::suggested);
            fields.neo4jTypes("supported", builder::supported);
            fields.bool("nullable", builder::nullable);
            fields.integer("dimension", builder::dimension);
            fields.custom(builder::custom);
            return builder.build();
        }

        ForeignKey foreignKey(Fields fields) {
            ForeignKey.Builder builder = ForeignKey.builder(
                    fields.requiredStrings("columns"), fields.requiredObject("references", this::foreignKeyReference));
            fields.custom(builder::custom);
            return builder.build();
        }

        ForeignKeyReference foreignKeyReference(Fields fields) {
            ForeignKeyReference.Builder builder = ForeignKeyReference.builder(fields.requiredString("table"));
            fields.strings("columns", builder::columns);
            fields.custom(builder::custom);
            return builder.build();
        }

        Mapping mapping(Fields fields) {
            String type = fields.requiredString("type");
            switch (type) {
                case MappingType.NODE: {
                    NodeMapping.Builder builder = NodeMapping.builder(
                            fields.requiredString("node"),
                            fields.requiredString("table"),
                            fields.requiredMap("properties", this::propertyMapping));
                    fields.enumValue("mode", MappingMode.class, builder::mode);
                    fields.string("match_label", builder::matchLabel);
                    fields.strings("key", builder::key);
                    fields.string("depends_on", builder::dependsOn);
                    fields.string("id", builder::id);
                    return builder.build();
                }
                case MappingType.QUERY: {
                    QueryMapping.Builder builder =
                            QueryMapping.builder(fields.requiredString("table"), fields.requiredString("query"));
                    fields.string("depends_on", builder::dependsOn);
                    fields.string("id", builder::id);
                    return builder.build();
                }
                case MappingType.RELATIONSHIP: {
                    RelationshipMapping.Builder builder = RelationshipMapping.builder(
                            fields.requiredString("relationship"),
                            fields.requiredString("table"),
                            fields.requiredObject("from_node", this::targetMapping),
                            fields.requiredObject("to_node", this::targetMapping));
                    fields.string("match_label", builder::matchLabel);
                    fields.map("properties", this::propertyMapping, builder::properties);
                    fields.enumValue("mode", MappingMode.class, builder::mode);
                    fields.strings("key", builder::key);
                    fields.string("depends_on", builder::dependsOn);
                    fields.string("id", builder::id);
                    return builder.build();
                }
                default:
                    throw fields.error("type", "Unknown mapping type '" + type + "'");
            }
        }

        TargetMapping targetMapping(Fields fields) {
            TargetMapping.Builder builder = TargetMapping.builder();
            fields.string("label", builder::label);
            fields.map("properties", this::propertyMapping, builder::properties);
            return builder.build();
        }

        PropertyMapping propertyMapping(Fields fields) {
            return new PropertyMapping(fields.requiredString("column"));
        }

        Fields fields(JsonNode json, String path) {
            if (absent(json)) return null;
            if (!json.isObject()) throw error(path, "Expected an object but found " + describe(json));
            return new Fields((ObjectNode) json, path);
        }

        <T> List<T> list(JsonNode json, String path, Function<Fields, T> decoder) {
            if (!json.isArray()) throw error(path, "Expected an array but found " + describe(json));
            List<T> values = new ArrayList<>();
            int index = 0;
            for (JsonNode item : json) {
                String itemPath = path + "[" + index++ + "]";
                Fields fields = fields(item, itemPath);
                values.add(fields == null ? null : decoder.apply(fields));
            }
            return values;
        }

        Object customValue(JsonNode json) {
            if (absent(json)) return null;
            if (json.isObject()) {
                Map<String, Object> map = new LinkedHashMap<>();
                Iterator<Map.Entry<String, JsonNode>> entries = json.fields();
                while (entries.hasNext()) {
                    Map.Entry<String, JsonNode> entry = entries.next();
                    map.put(entry.getKey(), customValue(entry.getValue()));
                }
                return map;
            }
            if (json.isArray()) {
                List<Object> list = new ArrayList<>();
                for (JsonNode item : json) list.add(customValue(item));
                return list;
            }
            if (json.isTextual()) return json.textValue();
            if (json.isBoolean()) return json.booleanValue();
            if (json.isIntegralNumber()) {
                if (!longInts && json.canConvertToInt()) return json.intValue();
                if (json.canConvertToLong()) return json.longValue();
                return json.bigIntegerValue();
            }
            if (json.isBigDecimal()) return json.decimalValue();
            if (json.isFloatingPointNumber()) return json.doubleValue();
            if (json.isBinary()) return json.asText();
            return scalarText(json);
        }

        /** The fields of an object being decoded, with lenient scalar coercion and path-aware errors. */
        final class Fields {
            private final ObjectNode node;
            private final String path;

            Fields(ObjectNode node, String path) {
                this.node = node;
                this.path = path;
            }

            private JsonNode get(String key) {
                JsonNode value = node.get(key);
                return absent(value) ? null : value;
            }

            private String path(String key) {
                return path.isEmpty() ? key : path + "." + key;
            }

            IllegalArgumentException error(String key, String message) {
                return Decoder.error(path(key), message);
            }

            private IllegalArgumentException missing(String key) {
                return error(key, "Field '" + key + "' is required");
            }

            private <T> void scalar(String key, BiFunction<JsonNode, String, T> decoder, Consumer<? super T> setter) {
                JsonNode value = get(key);
                if (value == null) return;
                T decoded = decoder.apply(value, path(key));
                if (decoded != null) setter.accept(decoded);
            }

            void string(String key, Consumer<String> setter) {
                scalar(key, Decoder::string, setter);
            }

            String requiredString(String key) {
                JsonNode value = get(key);
                if (value == null) throw missing(key);
                return Decoder.string(value, path(key));
            }

            void integer(String key, Consumer<Integer> setter) {
                scalar(key, Decoder::integer, setter);
            }

            void floatValue(String key, Consumer<Float> setter) {
                scalar(
                        key,
                        (value, at) -> {
                            Double decoded = Decoder.doubleValue(value, at);
                            return decoded == null ? null : decoded.floatValue();
                        },
                        setter);
            }

            double requiredDouble(String key) {
                JsonNode value = get(key);
                Double decoded = value == null ? null : Decoder.doubleValue(value, path(key));
                if (decoded == null) throw missing(key);
                return decoded;
            }

            void bool(String key, Consumer<Boolean> setter) {
                scalar(key, Decoder::bool, setter);
            }

            <E extends Enum<E>> void enumValue(String key, Class<E> type, Consumer<E> setter) {
                scalar(key, (value, at) -> Decoder.enumValue(value, at, type), setter);
            }

            <E extends Enum<E>> E requiredEnum(String key, Class<E> type) {
                JsonNode value = get(key);
                if (value == null) throw missing(key);
                return Decoder.enumValue(value, path(key), type);
            }

            void neo4jType(String key, Consumer<Neo4jType> setter) {
                scalar(key, Decoder::neo4jType, setter);
            }

            void neo4jTypes(String key, Consumer<List<Neo4jType>> setter) {
                scalar(key, (value, at) -> scalars(value, at, Decoder::neo4jType), setter);
            }

            void strings(String key, Consumer<List<String>> setter) {
                scalar(key, (value, at) -> scalars(value, at, Decoder::string), setter);
            }

            List<String> requiredStrings(String key) {
                JsonNode value = get(key);
                if (value == null) throw missing(key);
                return scalars(value, path(key), Decoder::string);
            }

            void doubles(String key, Consumer<List<Double>> setter) {
                scalar(key, (value, at) -> scalars(value, at, Decoder::doubleValue), setter);
            }

            <T> void object(String key, Function<Fields, T> decoder, Consumer<T> setter) {
                Fields fields = fields(get(key), path(key));
                if (fields != null) setter.accept(decoder.apply(fields));
            }

            <T> T requiredObject(String key, Function<Fields, T> decoder) {
                Fields fields = fields(get(key), path(key));
                if (fields == null) throw missing(key);
                return decoder.apply(fields);
            }

            <T> void list(String key, Function<Fields, T> decoder, Consumer<List<T>> setter) {
                JsonNode value = get(key);
                if (value != null) setter.accept(Decoder.this.list(value, path(key), decoder));
            }

            <T> void map(String key, Function<Fields, T> decoder, Consumer<Map<String, T>> setter) {
                mapOf(
                        key,
                        (value, at) -> {
                            Fields fields = fields(value, at);
                            return fields == null ? null : decoder.apply(fields);
                        },
                        setter);
            }

            <T> Map<String, T> requiredMap(String key, Function<Fields, T> decoder) {
                if (get(key) == null) throw missing(key);
                List<Map<String, T>> result = new ArrayList<>(1);
                map(key, decoder, result::add);
                return result.get(0);
            }

            /** Decodes an object of values, each of which may be null. */
            <T> void mapOf(String key, BiFunction<JsonNode, String, T> decoder, Consumer<Map<String, T>> setter) {
                Fields fields = fields(get(key), path(key));
                if (fields == null) return;
                Map<String, T> values = new LinkedHashMap<>();
                Iterator<Map.Entry<String, JsonNode>> entries = fields.node.fields();
                while (entries.hasNext()) {
                    Map.Entry<String, JsonNode> entry = entries.next();
                    JsonNode value = entry.getValue();
                    values.put(
                            entry.getKey(), absent(value) ? null : decoder.apply(value, fields.path(entry.getKey())));
                }
                setter.accept(values);
            }

            @SuppressWarnings("unchecked")
            void custom(Consumer<Map<String, Object>> setter) {
                Fields fields = fields(get("custom"), path("custom"));
                if (fields != null) setter.accept((Map<String, Object>) customValue(fields.node));
            }
        }

        private static <T> List<T> scalars(JsonNode json, String path, BiFunction<JsonNode, String, T> decoder) {
            if (!json.isArray()) throw error(path, "Expected an array but found " + describe(json));
            List<T> values = new ArrayList<>();
            int index = 0;
            for (JsonNode item : json) {
                String itemPath = path + "[" + index++ + "]";
                values.add(absent(item) ? null : decoder.apply(item, itemPath));
            }
            return values;
        }

        private static String string(JsonNode json, String path) {
            if (json.isTextual()) return json.textValue();
            if (json.isValueNode() || json.isPojo()) return scalarText(json);
            throw error(path, "Expected a string but found " + describe(json));
        }

        private static Integer integer(JsonNode json, String path) {
            if (json.isNumber()) {
                if (json.isIntegralNumber() && !json.canConvertToInt()) {
                    throw error(path, "Integer " + json.asText() + " is out of range");
                }
                return json.intValue();
            }
            if (json.isTextual()) {
                String text = json.textValue().trim();
                if (text.isEmpty()) return null;
                try {
                    return Integer.valueOf(text);
                } catch (NumberFormatException e) {
                    throw error(path, "Expected an integer but found '" + json.textValue() + "'");
                }
            }
            throw error(path, "Expected an integer but found " + describe(json));
        }

        private static Double doubleValue(JsonNode json, String path) {
            if (json.isNumber()) return json.doubleValue();
            if (json.isTextual()) {
                String text = json.textValue().trim();
                if (text.isEmpty()) return null;
                try {
                    return Double.valueOf(text);
                } catch (NumberFormatException e) {
                    throw error(path, "Expected a number but found '" + json.textValue() + "'");
                }
            }
            throw error(path, "Expected a number but found " + describe(json));
        }

        private static Boolean bool(JsonNode json, String path) {
            if (json.isBoolean()) return json.booleanValue();
            if (json.isTextual()) {
                String text = json.textValue().trim();
                if (text.isEmpty()) return null;
                if (text.equalsIgnoreCase("true")) return true;
                if (text.equalsIgnoreCase("false")) return false;
                throw error(path, "Expected a boolean but found '" + json.textValue() + "'");
            }
            // Integers coerce like C: zero is false, anything else true
            if (json.isIntegralNumber()) return json.bigIntegerValue().signum() != 0;
            throw error(path, "Expected a boolean but found " + describe(json));
        }

        private static <E extends Enum<E>> E enumValue(JsonNode json, String path, Class<E> type) {
            E[] constants = type.getEnumConstants();
            if (json.isIntegralNumber() && json.canConvertToInt()) {
                int ordinal = json.intValue();
                if (ordinal >= 0 && ordinal < constants.length) return constants[ordinal];
            }
            String text = string(json, path);
            for (E constant : constants) {
                if (constant.name().equals(text)) return constant;
            }
            throw error(path, "Unknown " + type.getSimpleName() + " '" + text + "'");
        }

        private static Neo4jType neo4jType(JsonNode json, String path) {
            String text = string(json, path);
            Neo4jType type = Neo4jType.fromString(text);
            if (type == null) throw error(path, "Unknown Neo4jType '" + text + "'");
            return type;
        }

        private static IllegalArgumentException error(String path, String message) {
            return new IllegalArgumentException(path.isEmpty() ? message : message + " at '" + path + "'");
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Shared
    // ---------------------------------------------------------------------------------------------------------------

    static boolean absent(JsonNode json) {
        return json == null || json.isNull() || json.isMissingNode();
    }

    /** The text of a scalar node, including the raw text of lenient values that aren't valid JSON numbers. */
    static String scalarText(JsonNode json) {
        if (json instanceof POJONode) {
            Object pojo = ((POJONode) json).getPojo();
            if (pojo instanceof RawValue) return String.valueOf(((RawValue) pojo).rawValue());
            return String.valueOf(pojo);
        }
        return json.asText();
    }

    private static String describe(JsonNode json) {
        return json.getNodeType().name().toLowerCase(Locale.ROOT);
    }
}
