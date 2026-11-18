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
package org.neo4j.spec.graph.migrate.migration.dataModel;

import static org.neo4j.spec.graph.migrate.migration.dataModel.DataModelUtils.nullIfEmpty;
import static org.neo4j.spec.graph.migrate.migration.dataModel.DataModelUtils.schemaMapOf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.neo4j.spec.graph.codec.schema.SchemaElement;
import org.neo4j.spec.graph.codec.schema.SchemaList;
import org.neo4j.spec.graph.codec.schema.SchemaLiteral;
import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.codec.schema.SchemaNull;
import org.neo4j.spec.graph.migrate.Migration;
import org.neo4j.spec.graph.model.NameFormat;
import org.neo4j.spec.graph.model.Type;
import org.neo4j.spec.graph.model.Version;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.model.type.IndexType;

public final class GraphSpecDataModelV3Migration extends Migration {
    public GraphSpecDataModelV3Migration() {
        super(Type.GRAPH_SPEC, Version.V1_1, Type.DATA_MODEL, Version.DATA_MODEL_V3_0);
    }

    @Override
    public SchemaMap migrate(SchemaMap schema) {
        List<SchemaMap> constraints = new ArrayList<SchemaMap>();
        List<SchemaMap> indexes = new ArrayList<SchemaMap>();
        NodeData nodeData = convertNodes(schema, constraints, indexes);
        RelationshipData relData = convertRelationships(schema, constraints, indexes);
        List<SchemaMap> nodeLabels = nodeData == null ? Collections.<SchemaMap>emptyList() : nodeData.getLabelsMap();
        List<SchemaMap> relationshipTypes =
                relData == null ? Collections.<SchemaMap>emptyList() : relData.getTypesMap();
        List<SchemaMap> nodeObjectTypes =
                nodeData == null ? Collections.<SchemaMap>emptyList() : nodeData.getObjectTypes();
        List<SchemaMap> relationshipObjectTypes =
                relData == null ? Collections.<SchemaMap>emptyList() : relData.getObjectTypes();
        SchemaMap graphSchema = schemaMapOf(
                "nodeLabels", nodeLabels,
                "relationshipTypes", relationshipTypes,
                "nodeObjectTypes", nodeObjectTypes,
                "relationshipObjectTypes", relationshipObjectTypes,
                "constraints", constraints,
                "indexes", indexes);
        return schemaMapOf(
                "version", "3.0.0",
                "graphSchemaRepresentation", schemaMapOf("version", "1.0.0", "graphSchema", graphSchema),
                "graphSchemaExtensionsRepresentation", convertExtensions(schema),
                "graphMappingRepresentation", convertGraphMapping(schema),
                "configurations", schemaMapOf("idsToIgnore", Collections.<String>emptyList()));
    }

    private SchemaMap convertGraphMapping(SchemaMap schema) {
        Map<String, SchemaMap> relationships = schema.mapOfMapsOrNull("relationships");
        if (relationships == null) {
            relationships = Collections.emptyMap();
        }
        List<SchemaMap> nodeMappings = new ArrayList<SchemaMap>();
        List<SchemaMap> relationshipMappings = new ArrayList<SchemaMap>();
        for (SchemaMap mapping : mappings(schema.mapOrNull("extensions"))) {
            if (mapping.containsKey("node")) {
                nodeMappings.add(schemaMapOf(
                        "node", DataModelUtils.refOf(mapping.string("node")),
                        "tableName", mapping.literal("table"),
                        "propertyMappings", convertPropertyMappings(mapping.mapOfMapsOrNull("properties"))));
            } else if (mapping.containsKey("relationship")) {
                String relId = findRelationshipId(relationships, mapping);
                if (relId == null) {
                    continue;
                }
                relationshipMappings.add(schemaMapOf(
                        "relationship", DataModelUtils.refOf(relId),
                        "tableName", mapping.literal("table"),
                        "fromMappings",
                                nullIfEmpty(convertEntityMap(
                                        mapping.map("from_node").mapOfMapsOrNull("properties"))),
                        "toMappings",
                                nullIfEmpty(
                                        convertEntityMap(mapping.map("to_node").mapOfMapsOrNull("properties"))),
                        "propertyMappings", convertPropertyMappings(mapping.mapOfMapsOrNull("properties"))));
            } else {
                throw new IllegalStateException("Invalid mapping type, must be node or relationship.");
            }
        }
        SchemaMap extensions = schema.mapOrNull("extensions");
        return schemaMapOf(
                "dataSourceSchema",
                        convertSourceSchema(
                                extensions == null ? null : extensions.mapOfMapsOrNull("data_source_tables")),
                "nodeMappings", nodeMappings,
                "relationshipMappings", relationshipMappings);
    }

    public SchemaMap convertExtensions(SchemaMap schema) {
        return schemaMapOf(
                "nodeKeyProperties", convertMappingKeyProperties(schema, "node"),
                "relationshipKeyProperties", nullIfEmpty(convertMappingKeyProperties(schema, "relationship")));
    }

    public List<SchemaMap> convertMappingKeyProperties(SchemaMap schema, String singular) {
        Map<String, Set<String>> keyProperties = new LinkedHashMap<String, Set<String>>();
        for (var mapping : mappings(schema.mapOrNull("extensions"))) {
            String entity = mapping.stringOrNull(singular);
            if (entity == null) {
                continue;
            }
            SchemaList keys = mapping.listOrNull("key");
            if (keys == null) {
                continue;
            }
            if (!keys.isEmpty()) {
                Set<String> set = keyProperties.computeIfAbsent(entity, k -> new LinkedHashSet<String>());
                for (SchemaElement key : keys) {
                    set.add(((SchemaLiteral) key).getString());
                }
            }
        }
        List<SchemaMap> result = new ArrayList<SchemaMap>();
        for (var entry : keyProperties.entrySet()) {
            result.add(schemaMapOf(
                    singular,
                    DataModelUtils.refOf(entry.getKey()),
                    "keyProperties",
                    DataModelUtils.refsOf(entry.getValue())));
        }
        return result;
    }

    /**
     * Finds the relationship ObjectType reference of a mapping, if the relationship exists
     */
    private String findRelationshipId(Map<String, SchemaMap> relationships, SchemaMap mapping) {
        String id = mapping.string("relationship");
        return relationships.containsKey(id) ? id : null;
    }

    private RelationshipData convertRelationships(
            SchemaMap schema, List<SchemaMap> constraints, List<SchemaMap> indexes) {
        Map<String, SchemaMap> relationships = schema.mapOfMapsOrNull("relationships");
        if (relationships == null) {
            return null;
        }
        List<SchemaMap> relationTypes = new ArrayList<SchemaMap>();
        List<SchemaMap> relationshipObjectTypes = new ArrayList<SchemaMap>();
        for (var entry : relationships.entrySet()) {
            var relId = entry.getKey();
            var rel = entry.getValue();
            String typeToken = rel.string("type");
            String typeId = "rt:" + relationTypes.size();
            relationTypes.add(schemaMapOf(
                    "$id", typeId,
                    "token", typeToken,
                    "properties", convertProperties(rel.mapOfMapsOrNull("properties"))));
            relationshipObjectTypes.add(schemaMapOf(
                    "$id", relId,
                    "type", DataModelUtils.refOf(typeId),
                    "from", DataModelUtils.refOf(rel.map("from").string("node")),
                    "to", DataModelUtils.refOf(rel.map("to").string("node")),
                    "description", rel.literalOrNull("description")));
            Map<String, String> propertyTokens = propertyTokens(rel.mapOfMapsOrNull("properties"));
            constraints.addAll(convertElements(
                    rel.mapOfMapsOrNull("constraints"),
                    "relationship",
                    typeId,
                    "constraintType",
                    GraphSpecDataModelV3Migration::constraintType,
                    (element, ids) -> constraintName(element, ids, propertyTokens, typeToken)));
            SchemaMap extensions = rel.mapOrNull("extensions");
            indexes.addAll(convertElements(
                    extensions == null ? null : extensions.mapOfMapsOrNull("indexes"),
                    "relationship",
                    typeId,
                    "indexType",
                    GraphSpecDataModelV3Migration::indexType,
                    (element, ids) -> indexName(element, ids, propertyTokens, typeToken)));
        }
        return new RelationshipData(relationTypes, relationshipObjectTypes, constraints, indexes);
    }

    private NodeData convertNodes(SchemaMap schema, List<SchemaMap> constraints, List<SchemaMap> indexes) {
        Map<String, SchemaMap> nodes = schema.mapOfMapsOrNull("nodes");
        if (nodes == null) {
            return null;
        }
        Map<String, String> nodeLabelsMap = new LinkedHashMap<String, String>();
        List<SchemaMap> nodeLabels = new ArrayList<SchemaMap>();
        List<SchemaMap> nodeObjectTypes = new ArrayList<SchemaMap>();
        for (var entry : nodes.entrySet()) {
            var nodeId = entry.getKey();
            var node = entry.getValue();
            SchemaMap labelsInfo = node.mapOrNull("labels");
            String primaryLabel = labelsInfo == null ? null : labelsInfo.stringOrNull("identifier");
            if (primaryLabel == null) {
                throw new IllegalStateException(
                        "Node at " + node.getPath() + " has no label, set label or labels.identifier");
            }
            List<String> allLabels = new ArrayList<String>();
            allLabels.add(primaryLabel);
            allLabels.addAll(labelStrings(labelsInfo.listOrNull("implied")));
            allLabels.addAll(labelStrings(labelsInfo.listOrNull("optional")));
            String primaryLabelId = "nl:null";
            List<SchemaMap> labelRefs = new ArrayList<SchemaMap>();
            for (var label : allLabels) {
                String labelId = nodeLabelsMap.get(label);
                if (labelId == null) {
                    labelId = "nl:" + nodeLabels.size();
                    nodeLabelsMap.put(label, labelId);
                    nodeLabels.add(schemaMapOf(
                            "$id", labelId,
                            "token", label,
                            "properties", convertProperties(node.mapOfMapsOrNull("properties"))));
                }
                if (Objects.equals(label, primaryLabel)) {
                    primaryLabelId = labelId;
                }
                labelRefs.add(DataModelUtils.refOf(labelId));
            }
            nodeObjectTypes.add(schemaMapOf(
                    "$id", nodeId,
                    "labels", labelRefs,
                    "description", node.literalOrNull("description")));
            Map<String, String> propertyTokens = propertyTokens(node.mapOfMapsOrNull("properties"));
            constraints.addAll(convertElements(
                    node.mapOfMapsOrNull("constraints"),
                    "node",
                    primaryLabelId,
                    "constraintType",
                    GraphSpecDataModelV3Migration::constraintType,
                    (element, ids) -> constraintName(element, ids, propertyTokens, primaryLabel)));
            SchemaMap extensions = node.mapOrNull("extensions");
            indexes.addAll(convertElements(
                    extensions == null ? null : extensions.mapOfMapsOrNull("indexes"),
                    "node",
                    primaryLabelId,
                    "indexType",
                    GraphSpecDataModelV3Migration::indexType,
                    (element, ids) -> indexName(element, ids, propertyTokens, primaryLabel)));
        }
        return new NodeData(nodeLabels, nodeObjectTypes, constraints, indexes);
    }

    private static List<String> labelStrings(SchemaList labels) {
        List<String> result = new ArrayList<String>();
        if (labels != null) {
            for (SchemaElement label : labels) {
                result.add(label.toString());
            }
        }
        return result;
    }

    /**
     * Mappings are grouped by id, the data model has no such grouping so they're flattened into a single list.
     */
    private List<SchemaMap> mappings(SchemaMap extensions) {
        List<SchemaMap> result = new ArrayList<SchemaMap>();
        Map<String, List<SchemaMap>> grouped =
                extensions == null ? null : extensions.mapOfListsOfMapsOrNull("data_source_mappings");
        if (grouped != null) {
            for (List<SchemaMap> group : grouped.values()) {
                result.addAll(group);
            }
        }
        return result;
    }

    private SchemaMap convertSourceSchema(Map<String, SchemaMap> tables) {
        if (tables == null) {
            return schemaMapOf("type", new SchemaNull(), "tableSchemas", Collections.<SchemaMap>emptyList());
        }
        List<SchemaMap> tableSchemas = new ArrayList<SchemaMap>();
        Object sourceType = null;
        for (var entry : tables.entrySet()) {
            var tableName = entry.getKey();
            var table = entry.getValue();
            if (sourceType == null) {
                sourceType = table.literalOrNull("source");
            }
            tableSchemas.add(schemaMapOf(
                    "name", tableName,
                    "expanded", true,
                    "fields", convertFields(table.listOfMapsOrNull("columns")),
                    "primaryKeys", table.listOrNull("primary_keys"),
                    "foreignKeys", convertForeignKeys(table.mapOfMapsOrNull("foreign_keys"))));
        }
        return schemaMapOf("type", sourceType != null ? sourceType : new SchemaNull(), "tableSchemas", tableSchemas);
    }

    public SchemaMap convertVisualisation(SchemaMap schema) {
        Map<String, SchemaMap> nodes = schema.mapOfMapsOrNull("nodes");
        List<SchemaMap> positions = new ArrayList<SchemaMap>();
        if (nodes != null) {
            for (var entry : nodes.entrySet()) {
                var id = entry.getKey();
                var node = entry.getValue();
                SchemaMap extensions = node.mapOrNull("extensions");
                SchemaMap pos = extensions == null ? null : extensions.mapOrNull("display");
                if (pos == null) {
                    continue;
                }
                positions.add(
                        schemaMapOf("id", id, "position", schemaMapOf("x", pos.literal("x"), "y", pos.literal("y"))));
            }
        }
        return schemaMapOf("nodes", nullIfEmpty(positions));
    }

    private Map<String, String> propertyTokens(Map<String, SchemaMap> properties) {
        Map<String, String> tokens = new LinkedHashMap<String, String>();
        if (properties != null) {
            for (var entry : properties.entrySet()) {
                String name = entry.getValue().stringOrNull("name");
                tokens.put(entry.getKey(), name != null ? name : entry.getKey());
            }
        }
        return tokens;
    }

    public List<SchemaMap> convertProperties(Map<String, SchemaMap> properties) {
        List<SchemaMap> result = new ArrayList<SchemaMap>();
        if (properties == null) {
            return result;
        }
        for (var entry : properties.entrySet()) {
            var propId = entry.getKey();
            var prop = entry.getValue();
            String name = prop.stringOrNull("name");
            result.add(schemaMapOf(
                    "$id",
                    propId,
                    "token",
                    name != null ? name : propId,
                    "type",
                    GraphSpecDataModelV3Migration.propertyType(prop.string("type"), prop.integerOrNull("dimension")),
                    "nullable",
                    false,
                    "description",
                    prop.literalOrNull("description")));
        }
        return result;
    }

    public List<SchemaMap> convertElements(
            Map<String, SchemaMap> elements,
            String entityType,
            String refId,
            String typeKey,
            Function<String, String> typeTransform,
            BiFunction<SchemaMap, List<String>, String> generatedName) {
        List<SchemaMap> result = new ArrayList<SchemaMap>();
        if (elements == null) {
            return result;
        }
        for (var entry : elements.entrySet()) {
            var name = entry.getKey();
            var element = entry.getValue();
            List<String> propertyIds = new ArrayList<String>();
            SchemaList properties = element.listOrNull("properties");
            if (properties != null) {
                for (SchemaElement propId : properties) {
                    propertyIds.add(((SchemaLiteral) propId).getString());
                }
            }
            String type = typeTransform.apply(element.string("type"));
            result.add(schemaMapOf(
                    "$id",
                    name,
                    "name",
                    resolvedName(element, propertyIds, () -> generatedName.apply(element, propertyIds)),
                    typeKey,
                    type,
                    "entityType",
                    entityType,
                    "nodeLabel",
                    entityType.equals("node") ? DataModelUtils.refOf(refId) : null,
                    "properties",
                    DataModelUtils.refsOf(propertyIds),
                    "relationshipType",
                    entityType.equals("relationship") ? DataModelUtils.refOf(refId) : null,
                    "options",
                    nullIfEmpty(element.mapOrNull("options"))));
        }
        return result;
    }

    private String resolvedName(SchemaMap schemaMap, List<String> propertyIds, Supplier<String> generate) {
        String name = DataModelUtils.nullIfBlank(schemaMap.stringOrNull("name"));
        if (name != null) {
            return name;
        }
        if (propertyIds.isEmpty()) {
            return "";
        }
        return generate.get();
    }

    private String constraintName(
            SchemaMap schemaMap, List<String> propertyIds, Map<String, String> propertyTokens, String entityToken) {
        String typeName = schemaMap.string("type");
        ConstraintType type = null;
        for (ConstraintType candidate : ConstraintType.values()) {
            if (candidate.name().equals(typeName)) {
                type = candidate;
                break;
            }
        }
        if (type == null) {
            throw new IllegalStateException(
                    "Unknown constraint type: '" + typeName + "' at " + schemaMap.getPath() + ".type");
        }
        return NameFormat.constraintName(tokens(propertyIds, propertyTokens), entityToken, type);
    }

    private String indexName(
            SchemaMap schemaMap, List<String> propertyIds, Map<String, String> propertyTokens, String entityToken) {
        String typeName = schemaMap.string("type");
        IndexType type = null;
        for (IndexType candidate : IndexType.values()) {
            if (candidate.name().equals(typeName)) {
                type = candidate;
                break;
            }
        }
        if (type == null) {
            throw new IllegalStateException(
                    "Unknown index type: '" + typeName + "' at " + schemaMap.getPath() + ".type");
        }
        return NameFormat.indexName(tokens(propertyIds, propertyTokens), entityToken, type);
    }

    private List<String> tokens(List<String> propertyIds, Map<String, String> propertyTokens) {
        List<String> result = new ArrayList<String>();
        for (String id : propertyIds) {
            String token = propertyTokens.get(id);
            result.add(token != null ? token : id);
        }
        return result;
    }

    public List<SchemaMap> convertFields(List<SchemaMap> fields) {
        List<SchemaMap> result = new ArrayList<SchemaMap>();
        if (fields == null) {
            return result;
        }
        for (var field : fields) {
            SchemaLiteral suggested = field.literalOrNull("suggested");
            SchemaList supported = field.listOrNull("supported");
            SchemaMap custom = field.mapOrNull("custom");
            Integer dimension = field.integerOrNull("dimension");
            List<SchemaElement> supportedTypes = null;
            if (supported != null) {
                supportedTypes = new ArrayList<SchemaElement>();
                for (SchemaElement type : supported) {
                    supportedTypes.add(
                            GraphSpecDataModelV3Migration.propertyType(((SchemaLiteral) type).getString(), dimension));
                }
            }
            result.add(schemaMapOf(
                    "name",
                    field.literalOrNull("name"),
                    "rawType",
                    field.literalOrNull("type"),
                    "size",
                    field.literalOrNull("size"),
                    "recommendedType",
                    suggested == null
                            ? null
                            : GraphSpecDataModelV3Migration.propertyType(suggested.getString(), dimension),
                    "supportedTypes",
                    supportedTypes,
                    "sample",
                    custom == null ? null : custom.literalOrNull("sample")));
        }
        return result;
    }

    public List<SchemaMap> convertForeignKeys(Map<String, SchemaMap> foreignKeys) {
        List<SchemaMap> result = new ArrayList<SchemaMap>();
        if (foreignKeys == null) {
            return result;
        }
        for (var fk : foreignKeys.values()) {
            List<String> columns = labelStrings(fk.list("columns"));
            SchemaMap references = fk.map("references");
            List<String> referencedColumns = labelStrings(references.list("columns"));
            List<SchemaMap> fieldMaps = new ArrayList<SchemaMap>();
            for (int i = 0; i < columns.size(); i++) {
                String referenced = i < referencedColumns.size() ? referencedColumns.get(i) : null;
                fieldMaps.add(schemaMapOf(
                        "field",
                        columns.get(i),
                        "referencedField",
                        referenced != null ? referenced : referencedColumns.get(referencedColumns.size() - 1)));
            }
            result.add(schemaMapOf("referencedTable", references.literal("table"), "fields", fieldMaps));
        }
        return result;
    }

    public List<SchemaMap> convertPropertyMappings(Map<String, SchemaMap> properties) {
        List<SchemaMap> result = new ArrayList<SchemaMap>();
        if (properties == null) {
            return result;
        }
        for (var entry : properties.entrySet()) {
            result.add(schemaMapOf(
                    "fieldName", entry.getValue().literal("column"),
                    "property", DataModelUtils.refOf(entry.getKey())));
        }
        return result;
    }

    public Map<String, Object> convertEntityMap(Map<String, SchemaMap> properties) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (properties == null) {
            return result;
        }
        for (var entry : properties.entrySet()) {
            result.put("#" + entry.getKey(), entry.getValue().literal("column"));
        }
        return result;
    }

    private static String type(String string) {
        String subject = string == null ? null : string.toUpperCase(Locale.ROOT);
        if (Objects.equals(subject, "LOCAL DATETIME")) {
            return "localdatetime";
        } else if (Objects.equals(subject, "ZONED DATETIME")) {
            return "datetime";
        } else if (Objects.equals(subject, "STRING")) {
            return "string";
        } else if (Objects.equals(subject, "INTEGER")) {
            return "integer";
        } else if (Objects.equals(subject, "FLOAT")) {
            return "float";
        } else if (Objects.equals(subject, "DATE")) {
            return "date";
        } else if (Objects.equals(subject, "BOOLEAN")) {
            return "boolean";
        } else if (Objects.equals(subject, "ANY")) {
            return null;
        } else if (Objects.equals(subject, "LOCAL TIME")) {
            return "localtime";
        } else if (Objects.equals(subject, "ZONED TIME")) {
            return "time";
        } else if (Objects.equals(subject, "POINT")) {
            return "point";
        } else {
            return string == null ? null : string.toLowerCase(Locale.ROOT);
        }
    }

    private static SchemaElement propertyType(String propertyType, Integer dimension) {
        if (Objects.equals(propertyType, "ANY")) {
            return new SchemaNull();
        } else if (propertyType != null && propertyType.startsWith("VECTOR")) {
            return schemaMapOf(
                    "type",
                    "vector",
                    "items",
                    schemaMapOf("type", GraphSpecDataModelV3Migration.type(innerType(propertyType, "VECTOR<"))),
                    "dimension",
                    dimension);
        } else if (propertyType != null && propertyType.startsWith("LIST")) {
            return schemaMapOf(
                    "type",
                    "array",
                    "items",
                    schemaMapOf("type", GraphSpecDataModelV3Migration.type(innerType(propertyType, "LIST<"))));
        } else {
            return schemaMapOf("type", GraphSpecDataModelV3Migration.type(propertyType));
        }
    }

    /**
     * The element type of e.g. {@code LIST<STRING>}, given the prefix up to and including the opening bracket.
     */
    private static String innerType(String propertyType, String prefix) {
        String inner = propertyType.startsWith(prefix) ? propertyType.substring(prefix.length()) : propertyType;
        return inner.endsWith(">") ? inner.substring(0, inner.length() - 1) : inner;
    }

    private static SchemaElement propertyType(String propertyType) {
        return GraphSpecDataModelV3Migration.propertyType(propertyType, null);
    }

    private static String constraintType(String name) {
        if (Objects.equals(name, "UNIQUE")) {
            return "uniqueness";
        } else if (Objects.equals(name, "EXISTS")) {
            return "propertyExistence";
        } else if (Objects.equals(name, "PROPERTY_TYPE")) {
            return "propertyType";
        } else if (Objects.equals(name, "KEY")) {
            return "key";
        } else {
            return name.toLowerCase(Locale.ROOT);
        }
    }

    private static String indexType(String name) {
        if (Objects.equals(name, "LOOKUP")) {
            return "lookup";
        } else if (Objects.equals(name, "RANGE")) {
            return "range";
        } else if (Objects.equals(name, "FULLTEXT")) {
            return "fullText";
        } else if (Objects.equals(name, "POINT")) {
            return "point";
        } else if (Objects.equals(name, "TEXT")) {
            return "text";
        } else if (Objects.equals(name, "VECTOR")) {
            return "vector";
        } else {
            return name.toLowerCase(Locale.ROOT);
        }
    }

    private static final class RelationshipData {
        private final List<SchemaMap> typesMap;

        private final List<SchemaMap> objectTypes;

        private final List<SchemaMap> constraints;

        private final List<SchemaMap> indexes;

        public RelationshipData(
                List<SchemaMap> typesMap,
                List<SchemaMap> objectTypes,
                List<SchemaMap> constraints,
                List<SchemaMap> indexes) {
            this.typesMap = typesMap;
            this.objectTypes = objectTypes;
            this.constraints = constraints;
            this.indexes = indexes;
        }

        public List<SchemaMap> getTypesMap() {
            return this.typesMap;
        }

        public List<SchemaMap> getObjectTypes() {
            return this.objectTypes;
        }

        public List<SchemaMap> getConstraints() {
            return this.constraints;
        }

        public List<SchemaMap> getIndexes() {
            return this.indexes;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof RelationshipData)) return false;
            RelationshipData that = (RelationshipData) other;
            return Objects.equals(typesMap, that.typesMap)
                    && Objects.equals(objectTypes, that.objectTypes)
                    && Objects.equals(constraints, that.constraints)
                    && Objects.equals(indexes, that.indexes);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(typesMap);
            result = 31 * result + Objects.hashCode(objectTypes);
            result = 31 * result + Objects.hashCode(constraints);
            result = 31 * result + Objects.hashCode(indexes);
            return result;
        }

        @Override
        public String toString() {
            return "RelationshipData(typesMap=" + typesMap + ", objectTypes=" + objectTypes + ", constraints="
                    + constraints + ", indexes=" + indexes + ")";
        }
    }

    private static final class NodeData {
        private final List<SchemaMap> labelsMap;

        private final List<SchemaMap> objectTypes;

        private final List<SchemaMap> constraints;

        private final List<SchemaMap> indexes;

        public NodeData(
                List<SchemaMap> labelsMap,
                List<SchemaMap> objectTypes,
                List<SchemaMap> constraints,
                List<SchemaMap> indexes) {
            this.labelsMap = labelsMap;
            this.objectTypes = objectTypes;
            this.constraints = constraints;
            this.indexes = indexes;
        }

        public List<SchemaMap> getLabelsMap() {
            return this.labelsMap;
        }

        public List<SchemaMap> getObjectTypes() {
            return this.objectTypes;
        }

        public List<SchemaMap> getConstraints() {
            return this.constraints;
        }

        public List<SchemaMap> getIndexes() {
            return this.indexes;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof NodeData)) return false;
            NodeData that = (NodeData) other;
            return Objects.equals(labelsMap, that.labelsMap)
                    && Objects.equals(objectTypes, that.objectTypes)
                    && Objects.equals(constraints, that.constraints)
                    && Objects.equals(indexes, that.indexes);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(labelsMap);
            result = 31 * result + Objects.hashCode(objectTypes);
            result = 31 * result + Objects.hashCode(constraints);
            result = 31 * result + Objects.hashCode(indexes);
            return result;
        }

        @Override
        public String toString() {
            return "NodeData(labelsMap=" + labelsMap + ", objectTypes=" + objectTypes + ", constraints=" + constraints
                    + ", indexes=" + indexes + ")";
        }
    }
}
