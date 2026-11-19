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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.neo4j.spec.graph.GraphSpecException;
import org.neo4j.spec.graph.model.ConstraintType;
import org.neo4j.spec.graph.model.ForeignKey;
import org.neo4j.spec.graph.model.ForeignKeyReference;
import org.neo4j.spec.graph.model.FulltextIndexOptions;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.GraphModelExtensions;
import org.neo4j.spec.graph.model.IndexOptions;
import org.neo4j.spec.graph.model.IndexType;
import org.neo4j.spec.graph.model.Labels;
import org.neo4j.spec.graph.model.LabelsExtensions;
import org.neo4j.spec.graph.model.Mapping;
import org.neo4j.spec.graph.model.MappingMode;
import org.neo4j.spec.graph.model.MappingType;
import org.neo4j.spec.graph.model.Neo4jType;
import org.neo4j.spec.graph.model.Node;
import org.neo4j.spec.graph.model.NodeConstraint;
import org.neo4j.spec.graph.model.NodeConstraintExtensions;
import org.neo4j.spec.graph.model.NodeDisplay;
import org.neo4j.spec.graph.model.NodeExtensions;
import org.neo4j.spec.graph.model.NodeIndex;
import org.neo4j.spec.graph.model.NodeMapping;
import org.neo4j.spec.graph.model.PointIndexOptions;
import org.neo4j.spec.graph.model.Property;
import org.neo4j.spec.graph.model.PropertyExtensions;
import org.neo4j.spec.graph.model.PropertyMapping;
import org.neo4j.spec.graph.model.QueryMapping;
import org.neo4j.spec.graph.model.Relationship;
import org.neo4j.spec.graph.model.RelationshipConstraint;
import org.neo4j.spec.graph.model.RelationshipConstraintExtensions;
import org.neo4j.spec.graph.model.RelationshipExtensions;
import org.neo4j.spec.graph.model.RelationshipIndex;
import org.neo4j.spec.graph.model.RelationshipMapping;
import org.neo4j.spec.graph.model.RelationshipTarget;
import org.neo4j.spec.graph.model.Table;
import org.neo4j.spec.graph.model.TableColumn;
import org.neo4j.spec.graph.model.TargetMapping;
import org.neo4j.spec.graph.model.VectorIndexOptions;

/**
 * Reads a {@link GraphModel} from a document tree of the latest spec version.
 *
 * <p>Shorthand is expanded: a node's {@code label} becomes {@code labels.identifier}, and a property's {@code key},
 * {@code unique} and {@code must_exist} flags become constraints appended after the longhand ones. {@code null},
 * empty and default values are treated as absent. The mapping between the tree and the model is written by hand,
 * without reflection.
 */
public final class ModelReader {

    private static final String[] NODE_KEYS = {
        "label", "labels", "properties", "constraints", "extensions", "description", "aliases", "reference"
    };
    private static final String[] REL_KEYS = {
        "type", "from", "to", "properties", "constraints", "extensions", "description", "aliases", "reference"
    };
    private static final String[] TARGET_KEYS = {"node", "label", "count", "min_count", "max_count"};
    private static final String[] PROPERTY_KEYS = {
        "type",
        "dimension",
        "must_exist",
        "unique",
        "key",
        "extensions",
        "name",
        "description",
        "aliases",
        "reference",
        "pattern",
        "one_of"
    };
    private static final String[] CONSTRAINT_KEYS = {"type", "properties", "extensions", "name"};
    private static final String[] INDEX_KEYS = {"type", "labels", "properties", "options", "custom", "name"};

    private final boolean failOnUnknownFields;

    /**
     * Creates a reader.
     *
     * @param failOnUnknownFields whether fields the specification doesn't define are an error
     */
    public ModelReader(boolean failOnUnknownFields) {
        this.failOnUnknownFields = failOnUnknownFields;
    }

    /**
     * Reads a document.
     *
     * @param document the document tree, expected to be an object
     * @return the model
     * @throws GraphSpecException if the document isn't a valid model, with the path of the problem
     */
    public GraphModel read(JsonNode document) {
        Obj o = new Obj(
                document, "", "schema", "version", "id", "name", "description", "nodes", "relationships", "extensions");
        GraphModel.Builder b = GraphModel.builder();
        b.schema(o.requiredString("schema"));
        b.version(o.requiredString("version"));
        b.id(o.requiredString("id"));
        b.name(o.string("name"));
        b.description(o.string("description"));
        for (Map.Entry<String, JsonNode> e : o.entries("nodes")) {
            b.node(e.getKey(), node(e.getKey(), new Obj(e.getValue(), "nodes." + e.getKey(), NODE_KEYS)));
        }
        for (Map.Entry<String, JsonNode> e : o.entries("relationships")) {
            b.relationship(
                    e.getKey(),
                    relationship(e.getKey(), new Obj(e.getValue(), "relationships." + e.getKey(), REL_KEYS)));
        }
        Obj ext = o.object("extensions", "data_source_tables", "data_source_mappings", "custom");
        if (ext != null) {
            b.extensions(graphExtensions(ext));
        }
        return b.build();
    }

    // ---- nodes -----------------------------------------------------------------------------------------------

    private Node node(String key, Obj o) {
        Node.Builder b = Node.builder();
        String shorthand = o.string("label");
        Obj labels = o.object("labels", "identifier", "implied", "optional", "extensions");
        String identifier = labels == null ? null : labels.string("identifier");
        if (shorthand != null && identifier != null && !shorthand.equals(identifier)) {
            throw o.error("label", "conflicts with labels.identifier");
        }
        if (identifier == null) {
            identifier = shorthand;
        }
        List<String> implied = labels == null ? new ArrayList<>() : labels.strings("implied");
        List<String> optional = labels == null ? new ArrayList<>() : labels.strings("optional");
        Map<String, Object> labelsCustom = labels == null ? new LinkedHashMap<>() : customOf(labels, "extensions");
        if (identifier != null || !implied.isEmpty() || !optional.isEmpty() || !labelsCustom.isEmpty()) {
            Labels.Builder lb =
                    Labels.builder().identifier(identifier).implied(implied).optional(optional);
            if (!labelsCustom.isEmpty()) {
                lb.extensions(LabelsExtensions.builder().custom(labelsCustom).build());
            }
            b.labels(lb.build());
        }
        Map<String, NodeConstraint> constraints = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> e : o.entries("constraints")) {
            constraints.put(
                    e.getKey(),
                    nodeConstraint(new Obj(e.getValue(), o.child("constraints." + e.getKey()), CONSTRAINT_KEYS)));
        }
        String owner = identifier == null ? key : identifier;
        for (Map.Entry<String, JsonNode> e : o.entries("properties")) {
            Obj p = new Obj(e.getValue(), o.child("properties." + e.getKey()), PROPERTY_KEYS);
            b.property(e.getKey(), property(p));
            for (Shorthand s : shorthands(p)) {
                constraints.put(
                        uniqueId(constraints.keySet(), s.id(key, e.getKey(), p)),
                        NodeConstraint.builder()
                                .type(s.type)
                                .property(e.getKey())
                                .name(s.name(e.getKey(), p, owner))
                                .build());
            }
        }
        b.constraints(constraints);
        Obj ext = o.object("extensions", "indexes", "display", "custom");
        if (ext != null) {
            NodeExtensions.Builder eb = NodeExtensions.builder();
            boolean any = false;
            for (Map.Entry<String, JsonNode> e : ext.entries("indexes")) {
                eb.index(e.getKey(), nodeIndex(new Obj(e.getValue(), ext.child("indexes." + e.getKey()), INDEX_KEYS)));
                any = true;
            }
            Obj display = ext.object("display", "x", "y", "colour", "custom");
            if (display != null) {
                eb.display(NodeDisplay.builder()
                        .x(display.requiredNumber("x"))
                        .y(display.requiredNumber("y"))
                        .colour(display.string("colour"))
                        .custom(display.custom("custom"))
                        .build());
                any = true;
            }
            Map<String, Object> custom = ext.custom("custom");
            eb.custom(custom);
            if (any || !custom.isEmpty()) {
                b.extensions(eb.build());
            }
        }
        b.description(o.string("description"));
        b.aliases(o.strings("aliases"));
        b.reference(o.string("reference"));
        return b.build();
    }

    private NodeConstraint nodeConstraint(Obj o) {
        NodeConstraint.Builder b = NodeConstraint.builder();
        b.type(o.requiredEnum("type", ConstraintType.values(), ConstraintType::value));
        b.properties(o.requiredStrings("properties"));
        Map<String, Object> custom = customOf(o, "extensions");
        if (!custom.isEmpty()) {
            b.extensions(NodeConstraintExtensions.builder().custom(custom).build());
        }
        b.name(o.string("name"));
        return b.build();
    }

    private NodeIndex nodeIndex(Obj o) {
        NodeIndex.Builder b = NodeIndex.builder();
        b.type(o.requiredEnum("type", IndexType.values(), IndexType::value));
        b.labels(o.requiredStrings("labels"));
        b.properties(o.requiredStrings("properties"));
        b.options(indexOptions(o));
        b.custom(o.custom("custom"));
        b.name(o.string("name"));
        return b.build();
    }

    private RelationshipIndex relationshipIndex(Obj o) {
        RelationshipIndex.Builder b = RelationshipIndex.builder();
        b.type(o.requiredEnum("type", IndexType.values(), IndexType::value));
        b.properties(o.requiredStrings("properties"));
        b.options(indexOptions(o));
        b.custom(o.custom("custom"));
        b.name(o.string("name"));
        return b.build();
    }

    private IndexOptions indexOptions(Obj index) {
        JsonNode raw = index.raw("options");
        if (raw == null) {
            return null;
        }
        String path = index.child("options");
        String type = new Obj(raw, path).requiredString("type");
        switch (type) {
            case "FULLTEXT": {
                Obj o = new Obj(
                        raw,
                        path,
                        "type",
                        "fulltext.default_analyzer",
                        "fulltext.analyzer",
                        "fulltext.eventually_consistent",
                        "fulltext.eventually_consistent_apply_parallelism",
                        "fulltext.eventually_consistent_refresh_interval",
                        "fulltext.eventually_consistent_refresh_parallelism",
                        "fulltext.eventually_consistent_index_update_queue_max_length");
                return FulltextIndexOptions.builder()
                        .defaultAnalyzer(o.string("fulltext.default_analyzer"))
                        .analyzer(o.string("fulltext.analyzer"))
                        .eventuallyConsistent(o.bool("fulltext.eventually_consistent"))
                        .eventuallyConsistentApplyParallelism(
                                o.integer("fulltext.eventually_consistent_apply_parallelism"))
                        .eventuallyConsistentRefreshInterval(
                                o.string("fulltext.eventually_consistent_refresh_interval"))
                        .eventuallyConsistentRefreshParallelism(
                                o.integer("fulltext.eventually_consistent_refresh_parallelism"))
                        .eventuallyConsistentIndexUpdateQueueMaxLength(
                                o.integer("fulltext.eventually_consistent_index_update_queue_max_length"))
                        .build();
            }
            case "POINT": {
                Obj o = new Obj(
                        raw,
                        path,
                        "type",
                        "spatial.cartesian.min",
                        "spatial.cartesian.max",
                        "spatial.cartesian-3d.min",
                        "spatial.cartesian-3d.max",
                        "spatial.wgs-84.min",
                        "spatial.wgs-84.max",
                        "spatial.wgs-84-3d.min",
                        "spatial.wgs-84-3d.max");
                return PointIndexOptions.builder()
                        .cartesianMin(o.numbers("spatial.cartesian.min"))
                        .cartesianMax(o.numbers("spatial.cartesian.max"))
                        .cartesian3dMin(o.numbers("spatial.cartesian-3d.min"))
                        .cartesian3dMax(o.numbers("spatial.cartesian-3d.max"))
                        .wgs84Min(o.numbers("spatial.wgs-84.min"))
                        .wgs84Max(o.numbers("spatial.wgs-84.max"))
                        .wgs843dMin(o.numbers("spatial.wgs-84-3d.min"))
                        .wgs843dMax(o.numbers("spatial.wgs-84-3d.max"))
                        .build();
            }
            case "VECTOR": {
                Obj o = new Obj(
                        raw,
                        path,
                        "type",
                        "vector.dimensions",
                        "vector.similarity_function",
                        "vector.default_search_expansion_factor",
                        "vector.quantization.enabled",
                        "vector.quantization.type",
                        "vector.hnsw.m",
                        "vector.hnsw.ef_construction");
                return VectorIndexOptions.builder()
                        .dimensions(o.integer("vector.dimensions"))
                        .similarityFunction(o.string("vector.similarity_function"))
                        .defaultSearchExpansionFactor(o.number("vector.default_search_expansion_factor"))
                        .quantizationEnabled(o.bool("vector.quantization.enabled"))
                        .quantizationType(o.string("vector.quantization.type"))
                        .hnswM(o.integer("vector.hnsw.m"))
                        .hnswEfConstruction(o.integer("vector.hnsw.ef_construction"))
                        .build();
            }
            default:
                throw new GraphSpecException(
                        "invalid value '" + type + "', must be one of [FULLTEXT, POINT, VECTOR]", path + ".type", null);
        }
    }

    // ---- properties and shorthand ----------------------------------------------------------------------------

    private Property property(Obj o) {
        Property.Builder b = Property.builder();
        Neo4jType type = o.enumValue("type", Neo4jType.values(), Neo4jType::value);
        // ANY is the default type, and means the same as no type
        b.type(type == Neo4jType.ANY ? null : type);
        b.dimension(o.integer("dimension"));
        Map<String, Object> custom = customOf(o, "extensions");
        if (!custom.isEmpty()) {
            b.extensions(PropertyExtensions.builder().custom(custom).build());
        }
        b.name(o.string("name"));
        b.aliases(o.strings("aliases"));
        b.description(o.string("description"));
        b.reference(o.string("reference"));
        b.pattern(o.string("pattern"));
        b.oneOf(o.strings("one_of"));
        return b.build();
    }

    /** A constraint written as a flag on a property. */
    private static final class Shorthand {
        final ConstraintType type;
        final String idPrefix;
        final String nameSuffix;

        Shorthand(ConstraintType type, String idPrefix, String nameSuffix) {
            this.type = type;
            this.idPrefix = idPrefix;
            this.nameSuffix = nameSuffix;
        }

        /** Like {@code exists_person_name}: type, owner key and the property's database name (its key by default). */
        String id(String ownerKey, String propertyKey, Obj property) {
            return idPrefix + "_" + ownerKey + "_" + displayName(propertyKey, property);
        }

        /** Like {@code name_Person_exists}: underscores in the property name and owner are doubled. */
        String name(String propertyKey, Obj property, String owner) {
            return displayName(propertyKey, property).replace("_", "__") + "_" + owner.replace("_", "__") + "_"
                    + nameSuffix;
        }

        private static String displayName(String propertyKey, Obj property) {
            String name = property.string("name");
            return name == null ? propertyKey : name;
        }
    }

    private static List<Shorthand> shorthands(Obj p) {
        List<Shorthand> list = new ArrayList<>();
        if (Boolean.TRUE.equals(p.bool("key"))) {
            list.add(new Shorthand(ConstraintType.KEY, "key", "key"));
        }
        if (Boolean.TRUE.equals(p.bool("unique"))) {
            list.add(new Shorthand(ConstraintType.UNIQUE, "unique", "uniq"));
        }
        if (Boolean.TRUE.equals(p.bool("must_exist"))) {
            list.add(new Shorthand(ConstraintType.EXISTS, "exists", "exists"));
        }
        return list;
    }

    private static String uniqueId(Set<String> taken, String id) {
        String candidate = id;
        for (int i = 2; taken.contains(candidate); i++) {
            candidate = id + "_" + i;
        }
        return candidate;
    }

    // ---- relationships ---------------------------------------------------------------------------------------

    private Relationship relationship(String key, Obj o) {
        Relationship.Builder b = Relationship.builder();
        String type = o.requiredString("type");
        b.type(type);
        b.from(target(o.requiredObject("from", TARGET_KEYS)));
        b.to(target(o.requiredObject("to", TARGET_KEYS)));
        Map<String, RelationshipConstraint> constraints = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> e : o.entries("constraints")) {
            Obj c = new Obj(e.getValue(), o.child("constraints." + e.getKey()), CONSTRAINT_KEYS);
            RelationshipConstraint.Builder cb = RelationshipConstraint.builder();
            cb.type(c.requiredEnum("type", ConstraintType.values(), ConstraintType::value));
            cb.properties(c.requiredStrings("properties"));
            Map<String, Object> custom = customOf(c, "extensions");
            if (!custom.isEmpty()) {
                cb.extensions(RelationshipConstraintExtensions.builder()
                        .custom(custom)
                        .build());
            }
            cb.name(c.string("name"));
            constraints.put(e.getKey(), cb.build());
        }
        for (Map.Entry<String, JsonNode> e : o.entries("properties")) {
            Obj p = new Obj(e.getValue(), o.child("properties." + e.getKey()), PROPERTY_KEYS);
            b.property(e.getKey(), property(p));
            for (Shorthand s : shorthands(p)) {
                constraints.put(
                        uniqueId(constraints.keySet(), s.id(key, e.getKey(), p)),
                        RelationshipConstraint.builder()
                                .type(s.type)
                                .property(e.getKey())
                                .name(s.name(e.getKey(), p, type))
                                .build());
            }
        }
        b.constraints(constraints);
        Obj ext = o.object("extensions", "indexes", "custom");
        if (ext != null) {
            RelationshipExtensions.Builder eb = RelationshipExtensions.builder();
            boolean any = false;
            for (Map.Entry<String, JsonNode> e : ext.entries("indexes")) {
                eb.index(
                        e.getKey(),
                        relationshipIndex(new Obj(e.getValue(), ext.child("indexes." + e.getKey()), INDEX_KEYS)));
                any = true;
            }
            Map<String, Object> custom = ext.custom("custom");
            eb.custom(custom);
            if (any || !custom.isEmpty()) {
                b.extensions(eb.build());
            }
        }
        b.description(o.string("description"));
        b.aliases(o.strings("aliases"));
        b.reference(o.string("reference"));
        return b.build();
    }

    private static RelationshipTarget target(Obj o) {
        return RelationshipTarget.builder()
                .node(o.string("node"))
                .label(o.string("label"))
                .count(o.integer("count"))
                .minCount(o.integer("min_count"))
                .maxCount(o.integer("max_count"))
                .build();
    }

    // ---- extensions: data sources ----------------------------------------------------------------------------

    private GraphModelExtensions graphExtensions(Obj o) {
        GraphModelExtensions.Builder b = GraphModelExtensions.builder();
        boolean any = false;
        for (Map.Entry<String, JsonNode> e : o.entries("data_source_tables")) {
            b.dataSourceTable(
                    e.getKey(),
                    table(new Obj(
                            e.getValue(),
                            o.child("data_source_tables." + e.getKey()),
                            "source",
                            "columns",
                            "primary_keys",
                            "foreign_keys",
                            "custom")));
            any = true;
        }
        for (Map.Entry<String, JsonNode> e : o.entries("data_source_mappings")) {
            String path = o.child("data_source_mappings." + e.getKey());
            if (!e.getValue().isArray()) {
                throw new GraphSpecException("expected a list", path, null);
            }
            List<Mapping> list = new ArrayList<>();
            int i = 0;
            for (JsonNode item : e.getValue()) {
                list.add(mapping(item, path + "." + i++));
            }
            b.dataSourceMapping(e.getKey(), list);
            any = true;
        }
        Map<String, Object> custom = o.custom("custom");
        b.custom(custom);
        return any || !custom.isEmpty() ? b.build() : null;
    }

    private Table table(Obj o) {
        Table.Builder b = Table.builder();
        b.source(o.requiredString("source"));
        JsonNode columns = o.raw("columns");
        if (columns != null) {
            String base = o.child("columns");
            if (!columns.isArray()) {
                throw new GraphSpecException("expected a list", base, null);
            }
            int i = 0;
            for (JsonNode item : columns) {
                b.column(column(new Obj(
                        item,
                        base + "." + i++,
                        "name",
                        "type",
                        "size",
                        "suggested",
                        "supported",
                        "nullable",
                        "dimension",
                        "custom")));
            }
        }
        b.primaryKeys(o.strings("primary_keys"));
        for (Map.Entry<String, JsonNode> e : o.entries("foreign_keys")) {
            Obj fk = new Obj(e.getValue(), o.child("foreign_keys." + e.getKey()), "columns", "references", "custom");
            Obj ref = fk.requiredObject("references", "table", "columns", "custom");
            b.foreignKey(
                    e.getKey(),
                    ForeignKey.builder()
                            .columns(fk.requiredStrings("columns"))
                            .references(ForeignKeyReference.builder()
                                    .table(ref.requiredString("table"))
                                    .columns(ref.strings("columns"))
                                    .custom(ref.custom("custom"))
                                    .build())
                            .custom(fk.custom("custom"))
                            .build());
        }
        b.custom(o.custom("custom"));
        return b.build();
    }

    private static TableColumn column(Obj o) {
        Boolean nullable = o.bool("nullable");
        return TableColumn.builder()
                .name(o.string("name"))
                .type(o.string("type"))
                .size(o.integer("size"))
                .suggested(o.enumValue("suggested", Neo4jType.values(), Neo4jType::value))
                .supported(o.enums("supported", Neo4jType.values(), Neo4jType::value))
                .nullable(Boolean.TRUE.equals(nullable) ? null : nullable)
                .dimension(o.integer("dimension"))
                .custom(o.custom("custom"))
                .build();
    }

    private Mapping mapping(JsonNode item, String path) {
        MappingType type = new Obj(item, path).requiredEnum("type", MappingType.values(), MappingType::value);
        switch (type) {
            case NODE: {
                Obj o = new Obj(
                        item,
                        path,
                        "type",
                        "node",
                        "table",
                        "properties",
                        "mode",
                        "match_label",
                        "key",
                        "depends_on",
                        "id");
                if (o.raw("properties") == null) {
                    throw o.missing("properties");
                }
                return NodeMapping.builder()
                        .node(o.requiredString("node"))
                        .table(o.requiredString("table"))
                        .properties(propertyMappings(o))
                        .mode(mode(o))
                        .matchLabel(o.string("match_label"))
                        .key(o.strings("key"))
                        .dependsOn(emptyToNull(o.string("depends_on")))
                        .id(o.string("id"))
                        .build();
            }
            case RELATIONSHIP: {
                Obj o = new Obj(
                        item,
                        path,
                        "type",
                        "relationship",
                        "table",
                        "from_node",
                        "to_node",
                        "match_label",
                        "properties",
                        "mode",
                        "key",
                        "depends_on",
                        "id");
                return RelationshipMapping.builder()
                        .relationship(o.requiredString("relationship"))
                        .table(o.requiredString("table"))
                        .fromNode(targetMapping(o.requiredObject("from_node", "label", "properties")))
                        .toNode(targetMapping(o.requiredObject("to_node", "label", "properties")))
                        .matchLabel(o.string("match_label"))
                        .properties(propertyMappings(o))
                        .mode(mode(o))
                        .key(o.strings("key"))
                        .dependsOn(emptyToNull(o.string("depends_on")))
                        .id(o.string("id"))
                        .build();
            }
            default: {
                Obj o = new Obj(item, path, "type", "table", "query", "depends_on", "id");
                return QueryMapping.builder()
                        .table(o.requiredString("table"))
                        .query(o.requiredString("query"))
                        .dependsOn(emptyToNull(o.string("depends_on")))
                        .id(o.string("id"))
                        .build();
            }
        }
    }

    private static TargetMapping targetMapping(Obj o) {
        return TargetMapping.builder()
                .label(o.string("label"))
                .properties(propertyMappings(o))
                .build();
    }

    private static Map<String, PropertyMapping> propertyMappings(Obj o) {
        Map<String, PropertyMapping> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> e : o.entries("properties")) {
            Obj p = o.nested(e.getValue(), "properties." + e.getKey(), "column");
            result.put(
                    e.getKey(),
                    PropertyMapping.builder().column(p.requiredString("column")).build());
        }
        return result;
    }

    /** {@code MERGE} is the default, and means the same as no mode. */
    private static MappingMode mode(Obj o) {
        MappingMode mode = o.enumValue("mode", MappingMode.values(), MappingMode::value);
        return mode == MappingMode.MERGE ? null : mode;
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    private static Map<String, Object> customOf(Obj o, String extensionsKey) {
        Obj ext = o.object(extensionsKey, "custom");
        return ext == null ? new LinkedHashMap<>() : ext.custom("custom");
    }

    // ---- tree access -----------------------------------------------------------------------------------------

    /** An object in the tree, at a path, with typed access to its fields. */
    private final class Obj {
        private final JsonNode node;
        private final String path;

        /** Checks the node is an object and, if configured, that it only has the known fields. */
        Obj(JsonNode node, String path, String... known) {
            if (node == null || !node.isObject()) {
                throw new GraphSpecException("expected an object", path.isEmpty() ? null : path, null);
            }
            this.node = node;
            this.path = path;
            if (failOnUnknownFields && known.length > 0) {
                List<String> allowed = Arrays.asList(known);
                for (Map.Entry<String, JsonNode> e : node.properties()) {
                    if (!allowed.contains(e.getKey())) {
                        throw new GraphSpecException("unknown field", child(e.getKey()), null);
                    }
                }
            }
        }

        Obj nested(JsonNode value, String key, String... known) {
            return new Obj(value, child(key), known);
        }

        String child(String key) {
            return path.isEmpty() ? key : path + "." + key;
        }

        GraphSpecException error(String key, String message) {
            return new GraphSpecException(message, child(key), null);
        }

        GraphSpecException missing(String key) {
            return error(key, "required field is missing");
        }

        /** The field's value, or null when it is absent or null. */
        JsonNode raw(String key) {
            JsonNode v = node.get(key);
            return v == null || v.isNull() ? null : v;
        }

        String string(String key) {
            JsonNode v = raw(key);
            if (v == null) {
                return null;
            }
            if (!v.isTextual()) {
                throw error(key, "expected a string");
            }
            return v.textValue();
        }

        String requiredString(String key) {
            String s = string(key);
            if (s == null) {
                throw missing(key);
            }
            return s;
        }

        Boolean bool(String key) {
            JsonNode v = raw(key);
            if (v == null) {
                return null;
            }
            if (!v.isBoolean()) {
                throw error(key, "expected a boolean");
            }
            return v.booleanValue();
        }

        Integer integer(String key) {
            JsonNode v = raw(key);
            if (v == null) {
                return null;
            }
            if (!v.isIntegralNumber() || !v.canConvertToInt()) {
                throw error(key, "expected an integer");
            }
            return v.intValue();
        }

        Double number(String key) {
            JsonNode v = raw(key);
            if (v == null) {
                return null;
            }
            if (!v.isNumber()) {
                throw error(key, "expected a number");
            }
            return v.doubleValue();
        }

        Double requiredNumber(String key) {
            Double d = number(key);
            if (d == null) {
                throw missing(key);
            }
            return d;
        }

        List<Double> numbers(String key) {
            JsonNode v = raw(key);
            List<Double> list = new ArrayList<>();
            if (v == null) {
                return list;
            }
            if (!v.isArray()) {
                throw error(key, "expected a list");
            }
            int i = 0;
            for (JsonNode item : v) {
                if (!item.isNumber()) {
                    throw error(key + "." + i, "expected a number");
                }
                list.add(item.doubleValue());
                i++;
            }
            return list;
        }

        List<String> strings(String key) {
            JsonNode v = raw(key);
            List<String> list = new ArrayList<>();
            if (v == null) {
                return list;
            }
            if (!v.isArray()) {
                throw error(key, "expected a list");
            }
            int i = 0;
            for (JsonNode item : v) {
                if (!item.isTextual()) {
                    throw error(key + "." + i, "expected a string");
                }
                list.add(item.textValue());
                i++;
            }
            return list;
        }

        List<String> requiredStrings(String key) {
            if (raw(key) == null) {
                throw missing(key);
            }
            return strings(key);
        }

        <E> E enumValue(String key, E[] values, Function<E, String> wire) {
            String s = string(key);
            return s == null ? null : lookup(key, s, values, wire);
        }

        <E> E requiredEnum(String key, E[] values, Function<E, String> wire) {
            E e = enumValue(key, values, wire);
            if (e == null) {
                throw missing(key);
            }
            return e;
        }

        <E> List<E> enums(String key, E[] values, Function<E, String> wire) {
            List<E> list = new ArrayList<>();
            int i = 0;
            for (String s : strings(key)) {
                list.add(lookup(key + "." + i++, s, values, wire));
            }
            return list;
        }

        private <E> E lookup(String key, String s, E[] values, Function<E, String> wire) {
            List<String> allowed = new ArrayList<>();
            for (E e : values) {
                if (wire.apply(e).equals(s)) {
                    return e;
                }
                allowed.add(wire.apply(e));
            }
            throw error(key, "invalid value '" + s + "', must be one of " + allowed);
        }

        /** An optional nested object, or null when absent or null. */
        Obj object(String key, String... known) {
            JsonNode v = raw(key);
            return v == null ? null : new Obj(v, child(key), known);
        }

        Obj requiredObject(String key, String... known) {
            Obj o = object(key, known);
            if (o == null) {
                throw missing(key);
            }
            return o;
        }

        /** The entries of an optional map field. */
        List<Map.Entry<String, JsonNode>> entries(String key) {
            JsonNode v = raw(key);
            List<Map.Entry<String, JsonNode>> list = new ArrayList<>();
            if (v == null) {
                return list;
            }
            if (!v.isObject()) {
                throw error(key, "expected an object");
            }
            for (Map.Entry<String, JsonNode> e : v.properties()) {
                if (e.getValue() == null || e.getValue().isNull()) {
                    throw error(key + "." + e.getKey(), "expected an object, found null");
                }
                list.add(e);
            }
            return list;
        }

        /** Free-form custom data. */
        Map<String, Object> custom(String key) {
            JsonNode v = raw(key);
            Map<String, Object> map = new LinkedHashMap<>();
            if (v == null) {
                return map;
            }
            if (!v.isObject()) {
                throw error(key, "expected an object");
            }
            for (Map.Entry<String, JsonNode> e : v.properties()) {
                map.put(e.getKey(), plain(e.getValue(), child(key + "." + e.getKey())));
            }
            return map;
        }
    }

    private static Object plain(JsonNode n, String path) {
        if (n == null || n.isNull()) {
            throw new GraphSpecException("custom data can't contain null", path, null);
        }
        if (n.isBoolean()) {
            return n.booleanValue();
        }
        if (n.isTextual()) {
            return n.textValue();
        }
        if (n.isInt() || n.isShort()) {
            return n.intValue();
        }
        if (n.isLong()) {
            return n.longValue();
        }
        if (n.isBigInteger()) {
            BigInteger v = n.bigIntegerValue();
            if (v.bitLength() < 64) {
                return v.longValue();
            }
            return v;
        }
        if (n.isBigDecimal()) {
            return n.decimalValue();
        }
        if (n.isNumber()) {
            return n.doubleValue();
        }
        if (n.isArray()) {
            List<Object> list = new ArrayList<>();
            int i = 0;
            for (JsonNode item : n) {
                list.add(plain(item, path + "." + i++));
            }
            return list;
        }
        if (n.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> e : n.properties()) {
                map.put(e.getKey(), plain(e.getValue(), path + "." + e.getKey()));
            }
            return map;
        }
        throw new GraphSpecException("unsupported value", path, null);
    }
}
