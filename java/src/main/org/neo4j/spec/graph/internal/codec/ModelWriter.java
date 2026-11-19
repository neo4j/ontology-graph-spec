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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.neo4j.spec.graph.GraphSpecException;
import org.neo4j.spec.graph.model.ForeignKey;
import org.neo4j.spec.graph.model.FulltextIndexOptions;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.GraphModelExtensions;
import org.neo4j.spec.graph.model.IndexOptions;
import org.neo4j.spec.graph.model.Labels;
import org.neo4j.spec.graph.model.Mapping;
import org.neo4j.spec.graph.model.MappingMode;
import org.neo4j.spec.graph.model.Neo4jType;
import org.neo4j.spec.graph.model.Node;
import org.neo4j.spec.graph.model.NodeConstraint;
import org.neo4j.spec.graph.model.NodeDisplay;
import org.neo4j.spec.graph.model.NodeExtensions;
import org.neo4j.spec.graph.model.NodeIndex;
import org.neo4j.spec.graph.model.NodeMapping;
import org.neo4j.spec.graph.model.PointIndexOptions;
import org.neo4j.spec.graph.model.Property;
import org.neo4j.spec.graph.model.PropertyMapping;
import org.neo4j.spec.graph.model.QueryMapping;
import org.neo4j.spec.graph.model.Relationship;
import org.neo4j.spec.graph.model.RelationshipConstraint;
import org.neo4j.spec.graph.model.RelationshipExtensions;
import org.neo4j.spec.graph.model.RelationshipIndex;
import org.neo4j.spec.graph.model.RelationshipMapping;
import org.neo4j.spec.graph.model.RelationshipTarget;
import org.neo4j.spec.graph.model.Table;
import org.neo4j.spec.graph.model.TableColumn;
import org.neo4j.spec.graph.model.TargetMapping;
import org.neo4j.spec.graph.model.VectorIndexOptions;

/**
 * Writes a {@link GraphModel} as a canonical longhand document tree of the latest spec version.
 *
 * <p>Keys are in the order of the JSON Schema. Absent, empty and default values are left out, except the collections
 * and objects the schema requires. Numbers get the node types Jackson's parser gives for the same text, so trees
 * compare equal to parsed documents: integers are {@code IntNode} (or {@code LongNode} when large) and every other
 * number is a {@code DoubleNode}.
 */
public final class ModelWriter {

    private static final JsonNodeFactory F = JsonNodeFactory.instance;

    private ModelWriter() {}

    /**
     * Writes the model.
     *
     * @param model the model
     * @return the document tree
     * @throws GraphSpecException if the model holds custom data that can't be written, such as a null
     */
    public static ObjectNode write(GraphModel model) {
        ObjectNode o = F.objectNode();
        o.put("schema", model.schema());
        o.put("version", model.version());
        o.put("id", model.id());
        model.name().ifPresent(v -> o.put("name", v));
        model.description().ifPresent(v -> o.put("description", v));
        if (!model.nodes().isEmpty()) {
            ObjectNode nodes = o.putObject("nodes");
            for (Map.Entry<String, Node> e : model.nodes().entrySet()) {
                nodes.set(e.getKey(), node(e.getValue(), "nodes." + e.getKey()));
            }
        }
        if (!model.relationships().isEmpty()) {
            ObjectNode rels = o.putObject("relationships");
            for (Map.Entry<String, Relationship> e : model.relationships().entrySet()) {
                rels.set(e.getKey(), relationship(e.getValue(), "relationships." + e.getKey()));
            }
        }
        if (model.extensions().isPresent()) {
            ObjectNode ext = graphExtensions(model.extensions().get(), "extensions");
            if (ext.size() > 0) {
                o.set("extensions", ext);
            }
        }
        return o;
    }

    // ---- nodes and relationships -----------------------------------------------------------------------------

    private static ObjectNode node(Node node, String path) {
        ObjectNode o = F.objectNode();
        if (node.labels().isPresent()) {
            ObjectNode labels = labels(node.labels().get(), path + ".labels");
            if (labels.size() > 0) {
                o.set("labels", labels);
            }
        }
        properties(o, node.properties(), path);
        if (!node.constraints().isEmpty()) {
            ObjectNode constraints = o.putObject("constraints");
            for (Map.Entry<String, NodeConstraint> e : node.constraints().entrySet()) {
                NodeConstraint c = e.getValue();
                String p = path + ".constraints." + e.getKey();
                ObjectNode co = constraints.putObject(e.getKey());
                co.put("type", c.type().value());
                co.set("properties", strings(c.properties()));
                c.extensions().ifPresent(x -> extensionsCustom(co, x.custom(), p + ".extensions.custom"));
                c.name().ifPresent(v -> co.put("name", v));
            }
        }
        if (node.extensions().isPresent()) {
            ObjectNode ext = nodeExtensions(node.extensions().get(), path + ".extensions");
            if (ext.size() > 0) {
                o.set("extensions", ext);
            }
        }
        node.description().ifPresent(v -> o.put("description", v));
        if (!node.aliases().isEmpty()) {
            o.set("aliases", strings(node.aliases()));
        }
        node.reference().ifPresent(v -> o.put("reference", v));
        return o;
    }

    private static ObjectNode labels(Labels labels, String path) {
        ObjectNode o = F.objectNode();
        labels.identifier().ifPresent(v -> o.put("identifier", v));
        if (!labels.implied().isEmpty()) {
            o.set("implied", strings(labels.implied()));
        }
        if (!labels.optional().isEmpty()) {
            o.set("optional", strings(labels.optional()));
        }
        labels.extensions().ifPresent(x -> extensionsCustom(o, x.custom(), path + ".extensions.custom"));
        return o;
    }

    private static void properties(ObjectNode parent, Map<String, Property> properties, String path) {
        if (properties.isEmpty()) {
            return;
        }
        ObjectNode map = parent.putObject("properties");
        for (Map.Entry<String, Property> e : properties.entrySet()) {
            Property p = e.getValue();
            String pp = path + ".properties." + e.getKey();
            ObjectNode o = map.putObject(e.getKey());
            p.type().filter(v -> v != Neo4jType.ANY).ifPresent(v -> o.put("type", v.value()));
            p.dimension().ifPresent(v -> o.put("dimension", v));
            p.extensions().ifPresent(x -> extensionsCustom(o, x.custom(), pp + ".extensions.custom"));
            p.name().ifPresent(v -> o.put("name", v));
            p.description().ifPresent(v -> o.put("description", v));
            if (!p.aliases().isEmpty()) {
                o.set("aliases", strings(p.aliases()));
            }
            p.reference().ifPresent(v -> o.put("reference", v));
            p.pattern().ifPresent(v -> o.put("pattern", v));
            if (!p.oneOf().isEmpty()) {
                o.set("one_of", strings(p.oneOf()));
            }
        }
    }

    private static ObjectNode nodeExtensions(NodeExtensions ext, String path) {
        ObjectNode o = F.objectNode();
        if (!ext.indexes().isEmpty()) {
            ObjectNode indexes = o.putObject("indexes");
            for (Map.Entry<String, NodeIndex> e : ext.indexes().entrySet()) {
                NodeIndex i = e.getValue();
                String p = path + ".indexes." + e.getKey();
                ObjectNode io = indexes.putObject(e.getKey());
                io.put("type", i.type().value());
                io.set("labels", strings(i.labels()));
                io.set("properties", strings(i.properties()));
                i.options().ifPresent(v -> io.set("options", options(v)));
                custom(io, "custom", i.custom(), p + ".custom");
                i.name().ifPresent(v -> io.put("name", v));
            }
        }
        if (ext.display().isPresent()) {
            NodeDisplay d = ext.display().get();
            ObjectNode display = o.putObject("display");
            display.put("x", d.x().doubleValue());
            display.put("y", d.y().doubleValue());
            d.colour().ifPresent(v -> display.put("colour", v));
            custom(display, "custom", d.custom(), path + ".display.custom");
        }
        custom(o, "custom", ext.custom(), path + ".custom");
        return o;
    }

    private static ObjectNode relationship(Relationship r, String path) {
        ObjectNode o = F.objectNode();
        o.put("type", r.type());
        o.set("from", target(r.from()));
        o.set("to", target(r.to()));
        properties(o, r.properties(), path);
        if (!r.constraints().isEmpty()) {
            ObjectNode constraints = o.putObject("constraints");
            for (Map.Entry<String, RelationshipConstraint> e : r.constraints().entrySet()) {
                RelationshipConstraint c = e.getValue();
                String p = path + ".constraints." + e.getKey();
                ObjectNode co = constraints.putObject(e.getKey());
                co.put("type", c.type().value());
                co.set("properties", strings(c.properties()));
                c.extensions().ifPresent(x -> extensionsCustom(co, x.custom(), p + ".extensions.custom"));
                c.name().ifPresent(v -> co.put("name", v));
            }
        }
        if (r.extensions().isPresent()) {
            RelationshipExtensions ext = r.extensions().get();
            ObjectNode eo = F.objectNode();
            if (!ext.indexes().isEmpty()) {
                ObjectNode indexes = eo.putObject("indexes");
                for (Map.Entry<String, RelationshipIndex> e : ext.indexes().entrySet()) {
                    RelationshipIndex i = e.getValue();
                    String p = path + ".extensions.indexes." + e.getKey();
                    ObjectNode io = indexes.putObject(e.getKey());
                    io.put("type", i.type().value());
                    io.set("properties", strings(i.properties()));
                    i.options().ifPresent(v -> io.set("options", options(v)));
                    custom(io, "custom", i.custom(), p + ".custom");
                    i.name().ifPresent(v -> io.put("name", v));
                }
            }
            custom(eo, "custom", ext.custom(), path + ".extensions.custom");
            if (eo.size() > 0) {
                o.set("extensions", eo);
            }
        }
        r.description().ifPresent(v -> o.put("description", v));
        if (!r.aliases().isEmpty()) {
            o.set("aliases", strings(r.aliases()));
        }
        r.reference().ifPresent(v -> o.put("reference", v));
        return o;
    }

    private static ObjectNode target(RelationshipTarget t) {
        ObjectNode o = F.objectNode();
        t.node().ifPresent(v -> o.put("node", v));
        t.label().ifPresent(v -> o.put("label", v));
        t.count().ifPresent(v -> o.put("count", v));
        t.minCount().ifPresent(v -> o.put("min_count", v));
        t.maxCount().ifPresent(v -> o.put("max_count", v));
        return o;
    }

    // ---- index options -----------------------------------------------------------------------------------------

    private static ObjectNode options(IndexOptions options) {
        ObjectNode o = F.objectNode();
        o.put("type", options.type().value());
        if (options instanceof FulltextIndexOptions) {
            FulltextIndexOptions f = (FulltextIndexOptions) options;
            f.defaultAnalyzer().ifPresent(v -> o.put("fulltext.default_analyzer", v));
            f.analyzer().ifPresent(v -> o.put("fulltext.analyzer", v));
            f.eventuallyConsistent().ifPresent(v -> o.put("fulltext.eventually_consistent", v));
            f.eventuallyConsistentApplyParallelism()
                    .ifPresent(v -> o.put("fulltext.eventually_consistent_apply_parallelism", v));
            f.eventuallyConsistentRefreshInterval()
                    .ifPresent(v -> o.put("fulltext.eventually_consistent_refresh_interval", v));
            f.eventuallyConsistentRefreshParallelism()
                    .ifPresent(v -> o.put("fulltext.eventually_consistent_refresh_parallelism", v));
            f.eventuallyConsistentIndexUpdateQueueMaxLength()
                    .ifPresent(v -> o.put("fulltext.eventually_consistent_index_update_queue_max_length", v));
        } else if (options instanceof PointIndexOptions) {
            PointIndexOptions p = (PointIndexOptions) options;
            numbers(o, "spatial.cartesian.min", p.cartesianMin());
            numbers(o, "spatial.cartesian.max", p.cartesianMax());
            numbers(o, "spatial.cartesian-3d.min", p.cartesian3dMin());
            numbers(o, "spatial.cartesian-3d.max", p.cartesian3dMax());
            numbers(o, "spatial.wgs-84.min", p.wgs84Min());
            numbers(o, "spatial.wgs-84.max", p.wgs84Max());
            numbers(o, "spatial.wgs-84-3d.min", p.wgs843dMin());
            numbers(o, "spatial.wgs-84-3d.max", p.wgs843dMax());
        } else if (options instanceof VectorIndexOptions) {
            VectorIndexOptions v = (VectorIndexOptions) options;
            v.dimensions().ifPresent(x -> o.put("vector.dimensions", x));
            v.similarityFunction().ifPresent(x -> o.put("vector.similarity_function", x));
            v.defaultSearchExpansionFactor().ifPresent(x -> o.put("vector.default_search_expansion_factor", x));
            v.quantizationEnabled().ifPresent(x -> o.put("vector.quantization.enabled", x));
            v.quantizationType().ifPresent(x -> o.put("vector.quantization.type", x));
            v.hnswM().ifPresent(x -> o.put("vector.hnsw.m", x));
            v.hnswEfConstruction().ifPresent(x -> o.put("vector.hnsw.ef_construction", x));
        }
        return o;
    }

    private static void numbers(ObjectNode o, String key, List<Double> values) {
        if (values.isEmpty()) {
            return;
        }
        ArrayNode a = o.putArray(key);
        for (Double d : values) {
            a.add(d.doubleValue());
        }
    }

    // ---- extensions: data sources ------------------------------------------------------------------------------

    private static ObjectNode graphExtensions(GraphModelExtensions ext, String path) {
        ObjectNode o = F.objectNode();
        if (!ext.dataSourceTables().isEmpty()) {
            ObjectNode tables = o.putObject("data_source_tables");
            for (Map.Entry<String, Table> e : ext.dataSourceTables().entrySet()) {
                tables.set(e.getKey(), table(e.getValue(), path + ".data_source_tables." + e.getKey()));
            }
        }
        if (!ext.dataSourceMappings().isEmpty()) {
            ObjectNode mappings = o.putObject("data_source_mappings");
            for (Map.Entry<String, List<Mapping>> e : ext.dataSourceMappings().entrySet()) {
                ArrayNode list = mappings.putArray(e.getKey());
                for (Mapping m : e.getValue()) {
                    list.add(mapping(m));
                }
            }
        }
        custom(o, "custom", ext.custom(), path + ".custom");
        return o;
    }

    private static ObjectNode table(Table t, String path) {
        ObjectNode o = F.objectNode();
        o.put("source", t.source());
        if (!t.columns().isEmpty()) {
            ArrayNode columns = o.putArray("columns");
            int i = 0;
            for (TableColumn c : t.columns()) {
                ObjectNode co = columns.addObject();
                c.name().ifPresent(v -> co.put("name", v));
                c.type().ifPresent(v -> co.put("type", v));
                c.size().ifPresent(v -> co.put("size", v));
                c.suggested().ifPresent(v -> co.put("suggested", v.value()));
                if (!c.supported().isEmpty()) {
                    ArrayNode supported = co.putArray("supported");
                    for (Neo4jType type : c.supported()) {
                        supported.add(type.value());
                    }
                }
                c.nullable().filter(v -> !v).ifPresent(v -> co.put("nullable", false));
                c.dimension().ifPresent(v -> co.put("dimension", v));
                custom(co, "custom", c.custom(), path + ".columns." + i++ + ".custom");
            }
        }
        if (!t.primaryKeys().isEmpty()) {
            o.set("primary_keys", strings(t.primaryKeys()));
        }
        if (!t.foreignKeys().isEmpty()) {
            ObjectNode fks = o.putObject("foreign_keys");
            for (Map.Entry<String, ForeignKey> e : t.foreignKeys().entrySet()) {
                ForeignKey fk = e.getValue();
                String p = path + ".foreign_keys." + e.getKey();
                ObjectNode fo = fks.putObject(e.getKey());
                fo.set("columns", strings(fk.columns()));
                ObjectNode ref = fo.putObject("references");
                ref.put("table", fk.references().table());
                if (!fk.references().columns().isEmpty()) {
                    ref.set("columns", strings(fk.references().columns()));
                }
                custom(ref, "custom", fk.references().custom(), p + ".references.custom");
                custom(fo, "custom", fk.custom(), p + ".custom");
            }
        }
        custom(o, "custom", t.custom(), path + ".custom");
        return o;
    }

    private static ObjectNode mapping(Mapping m) {
        ObjectNode o = F.objectNode();
        o.put("type", m.type().value());
        if (m instanceof NodeMapping) {
            NodeMapping n = (NodeMapping) m;
            o.put("node", n.node());
            o.put("table", n.table());
            propertyMappings(o, n.properties(), true);
            mode(o, n.mode());
            n.matchLabel().ifPresent(v -> o.put("match_label", v));
            if (!n.key().isEmpty()) {
                o.set("key", strings(n.key()));
            }
        } else if (m instanceof RelationshipMapping) {
            RelationshipMapping r = (RelationshipMapping) m;
            o.put("relationship", r.relationship());
            o.put("table", r.table());
            o.set("from_node", targetMapping(r.fromNode()));
            o.set("to_node", targetMapping(r.toNode()));
            r.matchLabel().ifPresent(v -> o.put("match_label", v));
            propertyMappings(o, r.properties(), false);
            mode(o, r.mode());
            if (!r.key().isEmpty()) {
                o.set("key", strings(r.key()));
            }
        } else if (m instanceof QueryMapping) {
            QueryMapping q = (QueryMapping) m;
            o.put("table", q.table());
            o.put("query", q.query());
        }
        m.dependsOn().filter(v -> !v.isEmpty()).ifPresent(v -> o.put("depends_on", v));
        m.id().ifPresent(v -> o.put("id", v));
        return o;
    }

    private static ObjectNode targetMapping(TargetMapping t) {
        ObjectNode o = F.objectNode();
        t.label().ifPresent(v -> o.put("label", v));
        propertyMappings(o, t.properties(), false);
        return o;
    }

    private static void propertyMappings(ObjectNode parent, Map<String, PropertyMapping> properties, boolean required) {
        if (properties.isEmpty() && !required) {
            return;
        }
        ObjectNode map = parent.putObject("properties");
        for (Map.Entry<String, PropertyMapping> e : properties.entrySet()) {
            map.putObject(e.getKey()).put("column", e.getValue().column());
        }
    }

    private static void mode(ObjectNode o, Optional<MappingMode> mode) {
        mode.filter(v -> v != MappingMode.MERGE).ifPresent(v -> o.put("mode", v.value()));
    }

    // ---- values ------------------------------------------------------------------------------------------------

    private static ArrayNode strings(List<String> values) {
        ArrayNode a = F.arrayNode();
        for (String s : values) {
            a.add(s);
        }
        return a;
    }

    /** Puts the custom data under the key as an object, unless it is empty. */
    private static void custom(ObjectNode parent, String key, Map<String, Object> custom, String path) {
        if (custom.isEmpty()) {
            return;
        }
        ObjectNode wrapper = parent.putObject(key);
        for (Map.Entry<String, Object> e : custom.entrySet()) {
            wrapper.set(e.getKey(), plain(e.getValue(), path + "." + e.getKey()));
        }
    }

    /** Puts {@code extensions: {custom: ...}} unless the custom data is empty. */
    private static void extensionsCustom(ObjectNode parent, Map<String, Object> custom, String path) {
        if (custom.isEmpty()) {
            return;
        }
        custom(parent.putObject("extensions"), "custom", custom, path);
    }

    private static JsonNode plain(Object value, String path) {
        if (value == null) {
            throw new GraphSpecException("custom data can't contain null", path, null);
        }
        if (value instanceof Boolean) {
            return F.booleanNode((Boolean) value);
        }
        if (value instanceof String) {
            return F.textNode((String) value);
        }
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return F.numberNode(((Number) value).intValue());
        }
        if (value instanceof Long) {
            return F.numberNode((Long) value);
        }
        if (value instanceof BigInteger) {
            return F.numberNode((BigInteger) value);
        }
        if (value instanceof BigDecimal) {
            return F.numberNode((BigDecimal) value);
        }
        if (value instanceof Float) {
            return F.numberNode((Float) value);
        }
        if (value instanceof Number) {
            return F.numberNode(((Number) value).doubleValue());
        }
        if (value instanceof Map) {
            ObjectNode o = F.objectNode();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                String key = String.valueOf(e.getKey());
                o.set(key, plain(e.getValue(), path + "." + key));
            }
            return o;
        }
        if (value instanceof Collection) {
            ArrayNode a = F.arrayNode();
            int i = 0;
            for (Object item : (Collection<?>) value) {
                a.add(plain(item, path + "." + i++));
            }
            return a;
        }
        throw new GraphSpecException(
                "custom data can't contain a value of type " + value.getClass().getName(), path, null);
    }
}
