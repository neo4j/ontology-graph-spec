// Generated from org/neo4j/spec/graph/migrate/migration/dataModel/GraphSpecDataModelV3Migration.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.migrate.migration.dataModel;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.neo4j.spec.graph.codec.schema.SchemaElements;
import org.neo4j.spec.graph.codec.schema.SchemaList;
import org.neo4j.spec.graph.codec.schema.SchemaLiteral;
import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.codec.schema.SchemaNull;
import org.neo4j.spec.graph.internal.Kt;
import org.neo4j.spec.graph.internal.Pair;
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
        List<Map<String, Object>> value = nodeData == null ? null : nodeData.getLabelsMap();
        List<SchemaMap> value2 = relData == null ? null : relData.getTypesMap();
        List<SchemaMap> value3 = nodeData == null ? null : nodeData.getObjectTypes();
        List<SchemaMap> value4 = relData == null ? null : relData.getObjectTypes();
        return SchemaElements.schemaMapOf(new Pair<>("version", "3.0.0"), new Pair<>("graphSchemaRepresentation", SchemaElements.schemaMapOf(new Pair<>("version", "1.0.0"), new Pair<>("graphSchema", SchemaElements.schemaMapOf(new Pair<>("nodeLabels", value != null ? value : Kt.<Map<String, Object>>emptyList()), new Pair<>("relationshipTypes", value2 != null ? value2 : Kt.<SchemaMap>emptyList()), new Pair<>("nodeObjectTypes", value3 != null ? value3 : Kt.<SchemaMap>emptyList()), new Pair<>("relationshipObjectTypes", value4 != null ? value4 : Kt.<SchemaMap>emptyList()), new Pair<>("constraints", constraints), new Pair<>("indexes", indexes))))), new Pair<>("graphSchemaExtensionsRepresentation", convertExtensions(schema)), new Pair<>("graphMappingRepresentation", convertGraphMapping(schema)), new Pair<>("configurations", SchemaElements.schemaMapOf(new Pair<>("idsToIgnore", Kt.<String>emptyList()))));
    }

    private SchemaMap convertGraphMapping(SchemaMap schema) {
        Map<String, SchemaMap> relationships = Kt.orEmpty(schema.mapOfMapsOrNull("relationships"));
        SchemaMap receiver = schema.mapOrNull("extensions");
        List<SchemaMap> mappings = Kt.orEmpty(receiver == null ? null : mappings(receiver));
        var tmp = Kt.partition(Kt.mapNotNull(mappings, mapping -> {
            if (Kt.containsKey(mapping, "node")) {
                return new Pair<>("node", SchemaElements.schemaMapOf(new Pair<>("node", DataModelUtilsKt.refOf(mapping.string("node"))), new Pair<>("tableName", mapping.literal("table")), new Pair<>("propertyMappings", convertPropertyMappings(mapping.mapOfMapsOrNull("properties")))));
            } else if (Kt.containsKey(mapping, "relationship")) {
                String relId = findRelationshipId(relationships, mapping);
                if (relId == null) {
                    return null;
                }
                return new Pair<>("rel", SchemaElements.schemaMapOf(new Pair<>("relationship", DataModelUtilsKt.refOf(relId)), new Pair<>("tableName", mapping.literal("table")), SchemaElements.toNotEmpty("fromMappings", convertEntityMap(mapping.map("from_node").mapOfMapsOrNull("properties"))), SchemaElements.toNotEmpty("toMappings", convertEntityMap(mapping.map("to_node").mapOfMapsOrNull("properties"))), new Pair<>("propertyMappings", convertPropertyMappings(mapping.mapOfMapsOrNull("properties")))));
            } else {
                throw new IllegalStateException("Invalid mapping type, must be node or relationship.");
            }
        }), it -> Objects.equals(it.getFirst(), "node"));
        var nodeMappings = tmp.getFirst();
        var relationshipMappings = tmp.getSecond();
        SchemaMap receiver2 = schema.mapOrNull("extensions");
        return SchemaElements.schemaMapOf(new Pair<>("dataSourceSchema", convertSourceSchema(receiver2 == null ? null : receiver2.mapOfMapsOrNull("data_source_tables"))), new Pair<>("nodeMappings", Kt.map(nodeMappings, it -> it.getSecond())), new Pair<>("relationshipMappings", Kt.map(relationshipMappings, it -> it.getSecond())));
    }

    public SchemaMap convertExtensions(SchemaMap schema) {
        return SchemaElements.schemaMapOf(new Pair<>("nodeKeyProperties", convertMappingKeyProperties(schema, "node")), SchemaElements.toNotEmpty("relationshipKeyProperties", convertMappingKeyProperties(schema, "relationship")));
    }

    public List<SchemaMap> convertMappingKeyProperties(SchemaMap schema, String singular) {
        Map<String, Set<String>> keyProperties = new LinkedHashMap<String, Set<String>>();
        SchemaMap receiver = schema.mapOrNull("extensions");
        for (var mapping : Kt.orEmpty(receiver == null ? null : mappings(receiver))) {
            String entity = mapping.stringOrNull(singular);
            if (entity == null) {
                continue;
            }
            SchemaList keys = mapping.listOrNull("key");
            if (keys == null) {
                continue;
            }
            if (!keys.isEmpty()) {
                Set<String> set = Kt.getOrPut(keyProperties, entity, () -> new LinkedHashSet<String>());
                set.addAll(Kt.map(keys, id -> ((SchemaLiteral) id).getString()));
            }
        }
        return Kt.map(keyProperties, entry -> {
            var entity = entry.getKey();
            var keys = entry.getValue();
            return SchemaElements.schemaMapOf(new Pair<>(singular, DataModelUtilsKt.refOf(entity)), new Pair<>("keyProperties", Kt.map(keys, it -> DataModelUtilsKt.refOf(it))));
        });
    }

    /**
     * Finds the relationship ObjectType reference of a mapping, if the relationship exists
     */
    private String findRelationshipId(Map<String, SchemaMap> relationships, SchemaMap mapping) {
        String id = mapping.string("relationship");
        return relationships.containsKey(id) ? id : null;
    }

    private RelationshipData convertRelationships(SchemaMap schema, List<SchemaMap> constraints,
            List<SchemaMap> indexes) {
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
            relationTypes.add(SchemaElements.schemaMapOf(new Pair<>("$id", typeId), new Pair<>("token", typeToken), new Pair<>("properties", convertProperties(rel.mapOfMapsOrNull("properties")))));
            relationshipObjectTypes.add(SchemaElements.schemaMapOf(new Pair<>("$id", relId), new Pair<>("type", DataModelUtilsKt.refOf(typeId)), new Pair<>("from", DataModelUtilsKt.refOf(rel.map("from").string("node"))), new Pair<>("to", DataModelUtilsKt.refOf(rel.map("to").string("node"))), new Pair<>("description", rel.literalOrNull("description"))));
            Map<String, String> propertyTokens = propertyTokens(rel.mapOfMapsOrNull("properties"));
            constraints.addAll(convertElements(rel.mapOfMapsOrNull("constraints"), "relationship", typeId, "constraintType", GraphSpecDataModelV3Migration::constraintType, (element, ids) -> constraintName(element, ids, propertyTokens, typeToken)));
            SchemaMap receiver = rel.mapOrNull("extensions");
            indexes.addAll(convertElements(receiver == null ? null : receiver.mapOfMapsOrNull("indexes"), "relationship", typeId, "indexType", GraphSpecDataModelV3Migration::indexType, (element, ids) -> indexName(element, ids, propertyTokens, typeToken)));
        }
        return new RelationshipData(relationTypes, relationshipObjectTypes, constraints, indexes);
    }

    private NodeData convertNodes(SchemaMap schema, List<SchemaMap> constraints,
            List<SchemaMap> indexes) {
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
                throw new IllegalStateException("Node at " + node.getPath() + " has no label, set label or labels.identifier");
            }
            SchemaList receiver = labelsInfo.listOrNull("implied");
            var impliedLabelsOrNull = receiver == null ? null : Kt.map(receiver, it -> it.toString());
            List<String> impliedLabels = impliedLabelsOrNull != null ? impliedLabelsOrNull : Kt.<String>emptyList();
            SchemaList receiver2 = labelsInfo.listOrNull("optional");
            var optionalLabelsOrNull = receiver2 == null ? null : Kt.map(receiver2, it -> it.toString());
            List<String> optionalLabels = optionalLabelsOrNull != null ? optionalLabelsOrNull : Kt.<String>emptyList();
            List<String> allLabels = Kt.plusAll(Kt.plusAll(Kt.listOf(primaryLabel), impliedLabels), optionalLabels);
            String primaryLabelId = "nl:null";
            List<SchemaMap> mapped = new ArrayList<SchemaMap>();
            for (var label : allLabels) {
                String labelId = nodeLabelsMap.get(label);
                if (labelId == null) {
                    labelId = "nl:" + nodeLabels.size();
                    nodeLabelsMap.put(label, labelId);
                    nodeLabels.add(SchemaElements.schemaMapOf(new Pair<>("$id", labelId), new Pair<>("token", label), new Pair<>("properties", convertProperties(node.mapOfMapsOrNull("properties")))));
                }
                if (Objects.equals(label, primaryLabel)) {
                    primaryLabelId = labelId;
                }
                mapped.add(DataModelUtilsKt.refOf(labelId));
            }
            List<SchemaMap> labelRefs = mapped;
            nodeObjectTypes.add(SchemaElements.schemaMapOf(new Pair<>("$id", nodeId), new Pair<>("labels", labelRefs), new Pair<>("description", node.literalOrNull("description"))));
            Map<String, String> propertyTokens = propertyTokens(node.mapOfMapsOrNull("properties"));
            constraints.addAll(convertElements(node.mapOfMapsOrNull("constraints"), "node", primaryLabelId, "constraintType", GraphSpecDataModelV3Migration::constraintType, (element, ids) -> constraintName(element, ids, propertyTokens, primaryLabel)));
            SchemaMap receiver3 = node.mapOrNull("extensions");
            indexes.addAll(convertElements(receiver3 == null ? null : receiver3.mapOfMapsOrNull("indexes"), "node", primaryLabelId, "indexType", GraphSpecDataModelV3Migration::indexType, (element, ids) -> indexName(element, ids, propertyTokens, primaryLabel)));
        }
        return new NodeData(Kt.uncheckedCast(nodeLabels), nodeObjectTypes, constraints, indexes);
    }

    /**
     * Mappings are grouped by id, the data model has no such grouping so they're flattened into a single list.
     */
    private List<SchemaMap> mappings(SchemaMap schemaMap) {
        return Kt.flatten(Kt.orEmpty(schemaMap.mapOfListsOfMapsOrNull("data_source_mappings")).values());
    }

    private SchemaMap convertSourceSchema(Map<String, SchemaMap> tables) {
        if (tables == null) {
            return SchemaElements.schemaMapOf(new Pair<>("type", new SchemaNull()), new Pair<>("tableSchemas", Kt.<SchemaMap>emptyList()));
        }
        List<SchemaMap> tableSchemas = new ArrayList<SchemaMap>();
        Object sourceType = null;
        for (var entry : tables.entrySet()) {
            var tableName = entry.getKey();
            var table = entry.getValue();
            if (sourceType == null) {
                sourceType = table.literalOrNull("source");
            }
            tableSchemas.add(SchemaElements.schemaMapOf(new Pair<>("name", tableName), new Pair<>("expanded", true), new Pair<>("fields", convertFields(table.listOfMapsOrNull("columns"))), new Pair<>("primaryKeys", table.listOrNull("primary_keys")), new Pair<>("foreignKeys", convertForeignKeys(table.mapOfMapsOrNull("foreign_keys")))));
        }
        return SchemaElements.schemaMapOf(new Pair<>("type", sourceType != null ? sourceType : new SchemaNull()), new Pair<>("tableSchemas", tableSchemas));
    }

    public SchemaMap convertVisualisation(SchemaMap schema) {
        Map<String, SchemaMap> nodes = Kt.orEmpty(schema.mapOfMapsOrNull("nodes"));
        return SchemaElements.schemaMapOf(SchemaElements.toNotEmpty("nodes", Kt.mapNotNull(nodes, entry -> {
            var id = entry.getKey();
            var node = entry.getValue();
            SchemaMap receiver = node.mapOrNull("extensions");
            SchemaMap pos = receiver == null ? null : receiver.mapOrNull("display");
            if (pos == null) {
                return null;
            }
            return SchemaElements.schemaMapOf(new Pair<>("id", id), new Pair<>("position", SchemaElements.schemaMapOf(new Pair<>("x", pos.literal("x")), new Pair<>("y", pos.literal("y")))));
        })));
    }

    private Map<String, String> propertyTokens(Map<String, SchemaMap> properties) {
        return Kt.mapValues(Kt.orEmpty(properties), entry -> {
            var id = entry.getKey();
            var property = entry.getValue();
            String value = property.stringOrNull("name");
            return value != null ? value : id;
        });
    }

    public List<SchemaMap> convertProperties(Map<String, SchemaMap> properties) {
        if (Kt.isNullOrEmpty(properties)) {
            return Kt.<SchemaMap>emptyList();
        }
        return Kt.map(properties, entry -> {
            var propId = entry.getKey();
            var prop = entry.getValue();
            String value = prop.stringOrNull("name");
            SchemaMap map = SchemaElements.schemaMapOf(new Pair<>("$id", propId), new Pair<>("token", value != null ? value : propId), new Pair<>("type", GraphSpecDataModelV3Migration.propertyType(prop.string("type"), prop.integerOrNull("dimension"))), new Pair<>("nullable", false), new Pair<>("description", prop.literalOrNull("description")));
            return map;
        });
    }

    public List<SchemaMap> convertElements(Map<String, SchemaMap> elements, String entityType,
            String refId, String typeKey, Function<String, String> typeTransform,
            BiFunction<SchemaMap, List<String>, String> generatedName) {
        if (Kt.isNullOrEmpty(elements)) {
            return Kt.<SchemaMap>emptyList();
        }
        return Kt.map(elements, entry -> {
            var name = entry.getKey();
            var element = entry.getValue();
            SchemaList receiver = element.listOrNull("properties");
            var propertyIdsOrNull = receiver == null ? null : Kt.map(receiver, propId -> ((SchemaLiteral) propId).getString());
            List<String> propertyIds = propertyIdsOrNull != null ? propertyIdsOrNull : Kt.<String>emptyList();
            String type = typeTransform.apply(element.string("type"));
            return SchemaElements.schemaMapOf(new Pair<>("$id", name), new Pair<>("name", resolvedName(element, propertyIds, () -> generatedName.apply(element, propertyIds))), new Pair<>(typeKey, type), new Pair<>("entityType", entityType), new Pair<>("nodeLabel", Objects.equals(entityType, "node") ? DataModelUtilsKt.refOf(refId) : null), new Pair<>("properties", Kt.map(propertyIds, it -> DataModelUtilsKt.refOf(it))), new Pair<>("relationshipType", Objects.equals(entityType, "relationship") ? DataModelUtilsKt.refOf(refId) : null), SchemaElements.toNotEmpty("options", element.mapOrNull("options")));
        });
    }

    private String resolvedName(SchemaMap schemaMap, List<String> propertyIds,
            Supplier<String> generate) {
        String receiver = schemaMap.stringOrNull("name");
        String receiver2 = receiver == null ? null : !Kt.isBlank(receiver) ? receiver : null;
        if (receiver2 != null) {
            return receiver2;
        }
        if (propertyIds.isEmpty()) {
            return "";
        }
        return generate.get();
    }

    private String constraintName(SchemaMap schemaMap, List<String> propertyIds,
            Map<String, String> propertyTokens, String entityToken) {
        String typeName = schemaMap.string("type");
        ConstraintType type = Kt.find(Arrays.asList(ConstraintType.values()), it -> Objects.equals(it.name(), typeName));
        if (type == null) {
            throw new IllegalStateException("Unknown constraint type: '" + typeName + "' at " + schemaMap.getPath() + ".type");
        }
        return NameFormat.constraintName(tokens(propertyIds, propertyTokens), entityToken, type);
    }

    private String indexName(SchemaMap schemaMap, List<String> propertyIds,
            Map<String, String> propertyTokens, String entityToken) {
        String typeName = schemaMap.string("type");
        IndexType type = Kt.find(Arrays.asList(IndexType.values()), it -> Objects.equals(it.name(), typeName));
        if (type == null) {
            throw new IllegalStateException("Unknown index type: '" + typeName + "' at " + schemaMap.getPath() + ".type");
        }
        return NameFormat.indexName(tokens(propertyIds, propertyTokens), entityToken, type);
    }

    private List<String> tokens(List<String> receiver, Map<String, String> propertyTokens) {
        return Kt.map(receiver, it -> {
            String value = propertyTokens.get(it);
            return value != null ? value : it;
        });
    }

    public List<SchemaMap> convertFields(List<SchemaMap> fields) {
        if (Kt.isNullOrEmpty(fields)) {
            return Kt.<SchemaMap>emptyList();
        }
        return Kt.map(fields, field -> {
            SchemaLiteral receiver = field.literalOrNull("suggested");
            SchemaList receiver2 = field.listOrNull("supported");
            SchemaMap receiver3 = field.mapOrNull("custom");
            return SchemaElements.schemaMapOf(new Pair<>("name", field.literalOrNull("name")), new Pair<>("rawType", field.literalOrNull("type")), new Pair<>("size", field.literalOrNull("size")), new Pair<>("recommendedType", receiver == null ? null : GraphSpecDataModelV3Migration.propertyType(receiver.getString(), field.integerOrNull("dimension"))), new Pair<>("supportedTypes", receiver2 == null ? null : Kt.map(receiver2, it -> GraphSpecDataModelV3Migration.propertyType(((SchemaLiteral) it).getString(), field.integerOrNull("dimension")))), new Pair<>("sample", receiver3 == null ? null : receiver3.literalOrNull("sample")));
        });
    }

    public List<SchemaMap> convertForeignKeys(Map<String, SchemaMap> foreignKeys) {
        if (Kt.isNullOrEmpty(foreignKeys)) {
            return Kt.<SchemaMap>emptyList();
        }
        return Kt.map(foreignKeys.values(), fk -> {
            List<String> columns = Kt.map(fk.list("columns"), it -> it.toString());
            SchemaMap references = fk.map("references");
            List<String> referencedColumns = Kt.map(references.list("columns"), it -> it.toString());
            List<SchemaMap> fieldMaps = Kt.map(Kt.indices(columns), i -> {
                String value = Kt.getOrNull(referencedColumns, i);
                return SchemaElements.schemaMapOf(new Pair<>("field", columns.get(i)), new Pair<>("referencedField", value != null ? value : Kt.last(referencedColumns)));
            });
            return SchemaElements.schemaMapOf(new Pair<>("referencedTable", references.literal("table")), new Pair<>("fields", fieldMaps));
        });
    }

    public List<SchemaMap> convertPropertyMappings(Map<String, SchemaMap> properties) {
        if (Kt.isNullOrEmpty(properties)) {
            return Kt.<SchemaMap>emptyList();
        }
        return Kt.map(properties, entry -> {
            var propId = entry.getKey();
            var propDef = entry.getValue();
            return SchemaElements.schemaMapOf(new Pair<>("fieldName", propDef.literal("column")), new Pair<>("property", DataModelUtilsKt.refOf(propId)));
        });
    }

    public Map<String, Object> convertEntityMap(Map<String, SchemaMap> properties) {
        if (Kt.isNullOrEmpty(properties)) {
            return Kt.<String, Object>emptyMap();
        }
        return Kt.associate(properties.entrySet(), entry -> {
            var key = entry.getKey();
            var value = entry.getValue();
            return new Pair<>("#" + key, value.literal("column"));
        });
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
            return SchemaElements.schemaMapOf(new Pair<>("type", "vector"), new Pair<>("items", SchemaElements.schemaMapOf(new Pair<>("type", GraphSpecDataModelV3Migration.type(Kt.removeSuffix(Kt.removePrefix(propertyType, "VECTOR<"), ">"))))), new Pair<>("dimension", dimension));
        } else if (propertyType != null && propertyType.startsWith("LIST")) {
            return SchemaElements.schemaMapOf(new Pair<>("type", "array"), new Pair<>("items", SchemaElements.schemaMapOf(new Pair<>("type", GraphSpecDataModelV3Migration.type(Kt.removeSuffix(Kt.removePrefix(propertyType, "LIST<"), ">"))))));
        } else {
            return SchemaElements.schemaMapOf(new Pair<>("type", GraphSpecDataModelV3Migration.type(propertyType)));
        }
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

        public RelationshipData(List<SchemaMap> typesMap, List<SchemaMap> objectTypes,
                List<SchemaMap> constraints, List<SchemaMap> indexes) {
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
            return "RelationshipData(typesMap=" + typesMap + ", objectTypes=" + objectTypes + ", constraints=" + constraints + ", indexes=" + indexes + ")";
        }
    }

    private static final class NodeData {
        private final List<Map<String, Object>> labelsMap;

        private final List<SchemaMap> objectTypes;

        private final List<SchemaMap> constraints;

        private final List<SchemaMap> indexes;

        public NodeData(List<Map<String, Object>> labelsMap, List<SchemaMap> objectTypes,
                List<SchemaMap> constraints, List<SchemaMap> indexes) {
            this.labelsMap = labelsMap;
            this.objectTypes = objectTypes;
            this.constraints = constraints;
            this.indexes = indexes;
        }

        public List<Map<String, Object>> getLabelsMap() {
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
            return "NodeData(labelsMap=" + labelsMap + ", objectTypes=" + objectTypes + ", constraints=" + constraints + ", indexes=" + indexes + ")";
        }
    }
}
