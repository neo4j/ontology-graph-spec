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
import org.neo4j.spec.graph.codec.schema.SchemaElement;
import org.neo4j.spec.graph.codec.schema.SchemaLiteral;
import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.codec.schema.SchemaPrimitive;
import org.neo4j.spec.graph.migrate.Migration;
import org.neo4j.spec.graph.model.NameFormat;
import org.neo4j.spec.graph.model.Type;
import org.neo4j.spec.graph.model.Version;
import org.neo4j.spec.graph.model.extension.mapping.MappingType;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.model.type.IndexType;

/**
 * 3.0 -> Graph Spec 4.0
 */
public final class DataModelV3GraphSpecMigration extends Migration {
    public static final String MAPPING_ID = "mapping0";

    public DataModelV3GraphSpecMigration() {
        super(Type.DATA_MODEL, Version.DATA_MODEL_V3_0, Type.GRAPH_SPEC, Version.V1_1);
    }

    @Override
    public SchemaMap migrate(SchemaMap schema) {
        SchemaMap extensions = schema.mapOrNull("graphSchemaExtensionsRepresentation");
        Map<String, Set<String>> nodeKeys = keyProperties(extensions, "node");
        Map<String, Set<String>> relKeys = keyProperties(extensions, "relationship");
        SchemaMap representation = schema.mapOrNull("graphSchemaRepresentation");
        SchemaMap graphSchema = representation == null ? null : representation.mapOrNull("graphSchema");
        if (graphSchema == null) {
            return schemaMapOf(
                    "schema", Version.schemaUrl(Version.V1_1),
                    "version", "1.0.0",
                    "id", "graph-model",
                    "description", schema.literalOrNull("description"),
                    "nodes", Collections.emptyMap(),
                    "relationships", Collections.emptyMap(),
                    "extensions", nullIfEmpty(extensions(migrateTables(schema), Collections.<SchemaMap>emptyList())));
        }
        ElementsByEntity constraints = gatherWithNames(graphSchema, "constraints");
        ElementsByEntity indexes = gatherWithNames(graphSchema, "indexes");
        Map<String, SchemaMap> nodes = migrateNodes(graphSchema, constraints.nodes, indexes.nodes);
        convertVisualisations(schema, nodes);
        List<SchemaMap> mappings = new ArrayList<>(nodeMappings(schema, nodeKeys));
        mappings.addAll(relationshipMappings(schema, relKeys));
        return schemaMapOf(
                "schema", Version.schemaUrl(Version.V1_1),
                "version", "1.0.0",
                "id", "graph-model",
                "description", schema.literalOrNull("description"),
                "nodes", nodes,
                "relationships", migrateRelationships(graphSchema, constraints.relationships, indexes.relationships),
                "extensions", nullIfEmpty(extensions(migrateTables(schema), mappings)));
    }

    /**
     * Graph model level extensions, omitting any which are empty.
     */
    private Map<String, Object> extensions(Map<String, SchemaMap> tables, List<SchemaMap> mappings) {
        Map<String, Object> extensions = new LinkedHashMap<>();
        if (!tables.isEmpty()) {
            extensions.put("data_source_tables", tables);
        }
        if (!mappings.isEmpty()) {
            extensions.put(
                    "data_source_mappings",
                    Collections.singletonMap(DataModelV3GraphSpecMigration.MAPPING_ID, mappings));
        }
        return extensions;
    }

    private Map<String, Set<String>> keyProperties(SchemaMap extensions, String entity) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        List<SchemaMap> keyProperties =
                extensions == null ? null : extensions.listOfMapsOrNull(entity + "KeyProperties");
        if (keyProperties != null) {
            for (SchemaMap kp : keyProperties) {
                result.put(
                        DataModelUtils.ref(kp, entity),
                        new LinkedHashSet<>(DataModelUtils.refs(kp.listOfMaps("keyProperties"))));
            }
        }
        return result;
    }

    /**
     * Moves node positions into each node's extensions.
     */
    public void convertVisualisations(SchemaMap schema, Map<String, SchemaMap> nodes) {
        SchemaElement value = schema.remove("visualisation");
        SchemaMap visualisation = value instanceof SchemaMap ? (SchemaMap) value : null;
        if (visualisation == null) {
            return;
        }
        for (var vis : visualisation.listOfMaps("nodes")) {
            String ref = DataModelUtils.removeHash(vis.string("id"));
            SchemaMap node = nodes.get(ref);
            if (node == null) {
                throw new IllegalStateException("Unknown node " + ref);
            }
            SchemaMap position = vis.map("position");
            SchemaMap extensions = node.mapOrNull("extensions");
            if (extensions == null) {
                extensions = schemaMapOf();
                node.set("extensions", extensions);
            }
            extensions.set("display", schemaMapOf("x", position.literal("x"), "y", position.literal("y")));
        }
    }

    private ElementsByEntity gatherWithNames(SchemaMap schema, String key) {
        List<SchemaMap> elements = schema.listOfMapsOrNull(key);
        if (elements == null) {
            return new ElementsByEntity(
                    new LinkedHashMap<>(), new LinkedHashMap<>());
        }
        Map<String, List<SchemaMap>> nodeElements = new LinkedHashMap<>();
        Map<String, List<SchemaMap>> relationshipElements = new LinkedHashMap<>();
        for (var element : elements) {
            if ("node".equals(element.string("entityType"))) {
                nodeElements
                        .computeIfAbsent(DataModelUtils.ref(element, "nodeLabel"), k -> new ArrayList<>())
                        .add(element);
            } else {
                relationshipElements
                        .computeIfAbsent(
                                DataModelUtils.ref(element, "relationshipType"), k -> new ArrayList<>())
                        .add(element);
            }
        }
        for (var element : elements) {
            element.set("name", element.stringOrNull("name"));
        }
        return new ElementsByEntity(nodeElements, relationshipElements);
    }

    public Map<String, SchemaMap> migrateNodes(
            SchemaMap schema,
            Map<String, ? extends List<SchemaMap>> constraints,
            Map<String, ? extends List<SchemaMap>> indexes) {
        Map<String, SchemaMap> nodes = new LinkedHashMap<>();
        Map<String, SchemaMap> nodeLabels = byId(schema.listOfMapsOrNull("nodeLabels"));
        if (nodeLabels == null) {
            return nodes;
        }
        List<SchemaMap> nodeObjTypes = schema.listOfMapsOrNull("nodeObjectTypes");
        if (nodeObjTypes == null) {
            return nodes;
        }
        for (var nodeObject : nodeObjTypes) {
            List<String> labelRefs = DataModelUtils.refs(nodeObject.listOfMaps("labels"));
            List<SchemaMap> labels = new ArrayList<>();
            for (String ref : labelRefs) {
                SchemaMap label = nodeLabels.get(ref);
                if (label == null) {
                    throw new IllegalStateException("Label " + ref + " not found");
                }
                labels.add(label);
            }
            List<String> tokens = new ArrayList<>();
            Map<String, String> propertyTokens = new LinkedHashMap<>();
            for (SchemaMap label : labels) {
                tokens.add(label.string("token"));
                List<SchemaMap> properties = label.listOfMapsOrNull("properties");
                if (properties != null) {
                    for (SchemaMap property : properties) {
                        propertyTokens.put(DataModelUtils.id(property), property.string("token"));
                    }
                }
            }
            String labelRef = labelRefs.isEmpty() ? null : labelRefs.get(0);
            String primaryLabel = tokens.get(0);
            String id = DataModelUtils.id(nodeObject);
            nodes.put(
                    id,
                    schemaMapOf(
                            "labels",
                                    schemaMapOf(
                                            "identifier",
                                            primaryLabel,
                                            "implied",
                                            nullIfEmpty(tokens.subList(1, tokens.size()))),
                            "constraints",
                                    nullIfEmpty(
                                            convertConstraints(constraints, labelRef, primaryLabel, propertyTokens)),
                            "properties", nullIfEmpty(convertProperties(labels)),
                            "extensions",
                                    indexExtensions(convertIndexes(indexes, labelRef, primaryLabel, propertyTokens)),
                            "description", nodeObject.literalOrNull("description")));
        }
        return nodes;
    }

    private static Map<String, SchemaMap> byId(List<SchemaMap> schemaMaps) {
        if (schemaMaps == null) {
            return null;
        }
        Map<String, SchemaMap> result = new LinkedHashMap<>();
        for (SchemaMap schemaMap : schemaMaps) {
            result.put(DataModelUtils.id(schemaMap), schemaMap);
        }
        return result;
    }

    /**
     * Index extensions, if there are any indexes.
     */
    private static Map<String, Object> indexExtensions(Map<String, SchemaMap> indexes) {
        return indexes == null || indexes.isEmpty()
                ? null
                : Collections.<String, Object>singletonMap("indexes", indexes);
    }

    public Map<String, SchemaMap> convertIndexes(
            Map<String, ? extends List<SchemaMap>> indexes,
            String labelRef,
            String label,
            Map<String, String> propertyTokens) {
        List<SchemaMap> labelIndexes = indexes.get(labelRef);
        if (labelIndexes == null) {
            return null;
        }
        Map<String, SchemaMap> result = new LinkedHashMap<>();
        for (SchemaMap index : labelIndexes) {
            String id = DataModelUtils.id(index);
            IndexType resolvedType = indexType(index);
            List<SchemaMap> properties = index.listOfMapsOrNull("properties");
            SchemaMap options = index.mapOrNull("options");
            if (options != null) {
                options.set("type", resolvedType.name());
            }
            String name = DataModelUtils.nullIfBlank(index.stringOrNull("name"));
            result.put(
                    id,
                    schemaMapOf(
                            "type", resolvedType.name(),
                            "labels", Collections.singletonList(label),
                            "properties", properties == null ? null : DataModelUtils.refs(properties),
                            "options", nullIfEmpty(options),
                            "name",
                                    name != null
                                            ? name
                                            : NameFormat.indexName(
                                                    tokensFrom(properties, propertyTokens), label, resolvedType)));
        }
        return result;
    }

    private IndexType indexType(SchemaMap index) {
        String type = index.string("indexType");
        IndexType value = DataModelUtils.dataModelIndexTypeFrom(type);
        if (value == null) {
            throw new IllegalStateException(
                    "Unknown index type: '" + type + "' at " + index.getPath() + "." + index.string("name"));
        }
        return value;
    }

    public Map<String, SchemaMap> convertConstraints(
            Map<String, ? extends List<SchemaMap>> constraints,
            String labelRef,
            String label,
            Map<String, String> propertyTokens) {
        List<SchemaMap> labelConstraints = constraints.get(labelRef);
        if (labelConstraints == null) {
            return null;
        }
        Map<String, SchemaMap> result = new LinkedHashMap<>();
        for (SchemaMap constraint : labelConstraints) {
            List<SchemaMap> properties = constraint.listOfMapsOrNull("properties");
            ConstraintType constraintType = constraintType(constraint);
            if (properties != null && properties.size() > 1 && constraintType == ConstraintType.PROPERTY_TYPE) {
                throw new IllegalStateException("Type constraints not supported on multiple properties.");
            }
            String id = DataModelUtils.id(constraint);
            String name = DataModelUtils.nullIfBlank(constraint.stringOrNull("name"));
            result.put(
                    id,
                    schemaMapOf(
                            "type", constraintType.name(),
                            "properties", properties == null ? null : nullIfEmpty(DataModelUtils.refs(properties)),
                            "name",
                                    name != null
                                            ? name
                                            : generatedName(properties, propertyTokens, label, constraintType)));
        }
        return result;
    }

    private String generatedName(
            List<SchemaMap> properties,
            Map<String, String> propertyTokens,
            String label,
            ConstraintType constraintType) {
        return NameFormat.constraintName(tokensFrom(properties, propertyTokens), label, constraintType);
    }

    private List<String> tokensFrom(List<SchemaMap> properties, Map<String, String> propertyTokens) {
        List<String> tokens = new ArrayList<>();
        if (properties != null) {
            for (SchemaMap property : properties) {
                String ref = DataModelUtils.ref(property);
                String token = propertyTokens.get(ref);
                tokens.add(token != null ? token : ref);
            }
        }
        return tokens;
    }

    private ConstraintType constraintType(SchemaMap constraint) {
        String type = constraint.string("constraintType");
        ConstraintType value = DataModelUtils.dataModelConstraintTypeFrom(type);
        if (value == null) {
            throw new IllegalStateException("Unknown constraint type: " + type + " at " + constraint.getPath() + "."
                    + constraint.string("name"));
        }
        return value;
    }

    public Map<String, SchemaMap> migrateRelationships(
            SchemaMap schema,
            Map<String, ? extends List<SchemaMap>> constraints,
            Map<String, ? extends List<SchemaMap>> indexes) {
        Map<String, SchemaMap> relationships = new LinkedHashMap<>();
        Map<String, SchemaMap> relationshipTypes = byId(schema.listOfMapsOrNull("relationshipTypes"));
        if (relationshipTypes == null) {
            return relationships;
        }
        List<SchemaMap> relObjTypes = schema.listOfMapsOrNull("relationshipObjectTypes");
        if (relObjTypes == null) {
            return relationships;
        }
        for (var objectType : relObjTypes) {
            String typeRef = DataModelUtils.ref(objectType, "type");
            SchemaMap relationshipType = relationshipTypes.get(typeRef);
            if (relationshipType == null) {
                throw new IllegalStateException("RelationshipType " + typeRef + " not found");
            }
            String token = relationshipType.string("token");
            Map<String, String> propertyTokens = new LinkedHashMap<>();
            List<SchemaMap> typeProperties = relationshipType.listOfMapsOrNull("properties");
            if (typeProperties != null) {
                for (SchemaMap property : typeProperties) {
                    propertyTokens.put(DataModelUtils.id(property), property.string("token"));
                }
            }
            String id = DataModelUtils.id(objectType);
            relationships.put(
                    id,
                    schemaMapOf(
                            "type", token,
                            "from", schemaMapOf("node", DataModelUtils.ref(objectType, "from")),
                            "to", schemaMapOf("node", DataModelUtils.ref(objectType, "to")),
                            "properties", convertProperties(Collections.singletonList(relationshipType)),
                            "constraints", nullIfEmpty(convertConstraints(constraints, typeRef, token, propertyTokens)),
                            "extensions", indexExtensions(convertIndexes(indexes, typeRef, token, propertyTokens)),
                            "description", objectType.literalOrNull("description")));
        }
        return relationships;
    }

    public Map<String, SchemaMap> convertProperties(List<SchemaMap> labels) {
        Map<String, SchemaMap> result = new LinkedHashMap<>();
        for (SchemaMap label : labels) {
            for (SchemaMap property : label.listOfMaps("properties")) {
                SchemaMap typeObj = property.map("type");
                result.put(
                        DataModelUtils.id(property),
                        schemaMapOf(
                                "name", property.literalOrNull("token"),
                                "type", DataModelV3GraphSpecMigration.neo4jType(typeObj),
                                "dimension", DataModelV3GraphSpecMigration.dimension(typeObj),
                                "description", property.literalOrNull("description")));
            }
        }
        return result;
    }

    public List<SchemaMap> relationshipMappings(SchemaMap schema, Map<String, ? extends Set<String>> relKeys) {
        SchemaMap graph = schema.map("graphSchemaRepresentation").map("graphSchema");
        Map<String, SchemaMap> relInfo = byId(graph.listOfMapsOrNull("relationshipObjectTypes"));
        if (relInfo == null) {
            return Collections.emptyList();
        }
        List<SchemaMap> relationshipMappings =
                schema.map("graphMappingRepresentation").listOfMapsOrNull("relationshipMappings");
        if (relationshipMappings == null) {
            return Collections.emptyList();
        }
        List<SchemaMap> mappings = new ArrayList<>();
        for (var mapping : relationshipMappings) {
            String ref = DataModelUtils.ref(mapping, "relationship");
            if (!relInfo.containsKey(ref)) {
                throw new IllegalStateException("Relationship " + ref + " not found");
            }
            Set<String> keys = relKeys.get(ref);
            mappings.add(schemaMapOf(
                    "type", new SchemaLiteral(MappingType.RELATIONSHIP),
                    "relationship", ref,
                    "from_node", schemaMapOf("properties", entityMap(mapping, "fromMappings")),
                    "to_node", schemaMapOf("properties", entityMap(mapping, "toMappings")),
                    "table", mapping.literal("tableName"),
                    "properties", nullIfEmpty(migratePropertyMappings(mapping)),
                    "key",
                            nullIfEmpty(migrateKeyProperties(
                                    mapping, keys != null ? keys : Collections.<String>emptySet()))));
        }
        return mappings;
    }

    public Map<String, SchemaMap> entityMap(SchemaMap schemaMap, String key) {
        Map<String, SchemaMap> result = new LinkedHashMap<>();
        SchemaMap entity = schemaMap.mapOrNull(key);
        if (entity != null) {
            for (var entry : entity.entrySet()) {
                result.put(DataModelUtils.removeHash(entry.getKey()), schemaMapOf("column", entry.getValue()));
            }
        }
        return result;
    }

    public List<SchemaMap> nodeMappings(SchemaMap schema, Map<String, ? extends Set<String>> nodeKeys) {
        List<SchemaMap> nodeMappings = schema.map("graphMappingRepresentation").listOfMapsOrNull("nodeMappings");
        if (nodeMappings == null) {
            return Collections.emptyList();
        }
        List<SchemaMap> mappings = new ArrayList<>();
        for (var nodeMapping : nodeMappings) {
            String ref = DataModelUtils.ref(nodeMapping, "node");
            Set<String> keys = nodeKeys.get(ref);
            mappings.add(schemaMapOf(
                    "type", new SchemaLiteral(MappingType.NODE),
                    "node", ref,
                    "table", nodeMapping.literal("tableName"),
                    "properties", nullIfEmpty(migratePropertyMappings(nodeMapping)),
                    "key",
                            nullIfEmpty(migrateKeyProperties(
                                    nodeMapping, keys != null ? keys : Collections.<String>emptySet()))));
        }
        return mappings;
    }

    private Map<String, Map<String, SchemaPrimitive>> migratePropertyMappings(SchemaMap entity) {
        Map<String, Map<String, SchemaPrimitive>> result = new LinkedHashMap<>();
        for (SchemaMap mapping : entity.listOfMaps("propertyMappings")) {
            result.put(
                    DataModelUtils.ref(mapping, "property"),
                    Collections.singletonMap("column", mapping.literal("fieldName")));
        }
        return result;
    }

    private List<String> migrateKeyProperties(SchemaMap entity, Set<String> keys) {
        List<String> result = new ArrayList<>();
        for (SchemaMap mapping : entity.listOfMaps("propertyMappings")) {
            String ref = DataModelUtils.ref(mapping, "property");
            if (keys.contains(ref)) {
                result.add(ref);
            }
        }
        return result;
    }

    public Map<String, SchemaMap> migrateTables(SchemaMap schema) {
        Map<String, SchemaMap> tables = new LinkedHashMap<>();
        SchemaMap sourceSchema = schema.map("graphMappingRepresentation").mapOrNull("dataSourceSchema");
        if (sourceSchema == null) {
            return tables;
        }
        for (var table : sourceSchema.listOfMaps("tableSchemas")) {
            String name = table.string("name");
            tables.put(
                    name,
                    schemaMapOf(
                            "source", sourceSchema.literalOrNull("type"),
                            "columns", migrateColumns(table),
                            "primary_keys", nullIfEmpty(table.listOrNull("primaryKeys")),
                            "foreign_keys", nullIfEmpty(migrateForeignKeys(table))));
        }
        return tables;
    }

    private Map<String, SchemaElement> migrateForeignKeys(SchemaMap table) {
        List<SchemaMap> foreign = table.listOfMapsOrNull("foreignKeys");
        if (foreign == null) {
            return null;
        }
        Map<String, SchemaElement> foreignKeys = new LinkedHashMap<>();
        for (var foreignKey : foreign) {
            List<String> columns = new ArrayList<>();
            List<String> referencedColumns = new ArrayList<>();
            for (SchemaMap field : foreignKey.listOfMaps("fields")) {
                columns.add(field.string("field"));
                referencedColumns.add(field.string("referencedField"));
            }
            foreignKeys.put(
                    "foreignKey" + (foreignKeys.size() + 1),
                    schemaMapOf(
                            "columns",
                            columns,
                            "references",
                            schemaMapOf("table", foreignKey.literal("referencedTable"), "columns", referencedColumns)));
        }
        return foreignKeys;
    }

    private List<SchemaMap> migrateColumns(SchemaMap table) {
        List<SchemaMap> columns = new ArrayList<>();
        for (var field : table.listOfMaps("fields")) {
            List<SchemaMap> supportedTypes = field.listOfMapsOrNull("supportedTypes");
            Integer dimension = dimension(field.mapOrNull("recommendedType"));
            if (dimension == null && supportedTypes != null) {
                for (SchemaMap supportedType : supportedTypes) {
                    dimension = dimension(supportedType);
                    if (dimension != null) {
                        break;
                    }
                }
            }
            List<String> supported = null;
            if (supportedTypes != null) {
                supported = new ArrayList<>();
                for (SchemaMap supportedType : supportedTypes) {
                    supported.add(neo4jType(supportedType));
                }
            }
            SchemaLiteral sample = field.literalOrNull("sample");
            columns.add(schemaMapOf(
                    "name", field.literal("name"),
                    "type", field.literalOrNull("rawType"),
                    "size", field.literalOrNull("size"),
                    "suggested", neo4jType(field.mapOrNull("recommendedType")),
                    "supported", supported,
                    "dimension", dimension,
                    "custom", sample == null ? null : schemaMapOf("sample", sample)));
        }
        return columns;
    }

    private static String neo4jType(SchemaMap type) {
        String receiver = type == null ? null : type.stringOrNull("type");
        String base = receiver == null ? null : receiver.toLowerCase(Locale.ROOT);
        if (base == null) {
            return null;
        }
        if (Objects.equals(base, "array")) {
            String receiver2 = DataModelV3GraphSpecMigration.itemType(type);
            return receiver2 == null ? null : "LIST<" + receiver2 + ">";
        } else if (Objects.equals(base, "vector")) {
            String receiver2 = DataModelV3GraphSpecMigration.itemType(type);
            return receiver2 == null ? null : "VECTOR<" + receiver2 + ">";
        } else {
            return DataModelV3GraphSpecMigration.scalarType(base);
        }
    }

    private static Integer dimension(SchemaMap type) {
        String receiver = type == null ? null : type.stringOrNull("type");
        if (Objects.equals(receiver == null ? null : receiver.toLowerCase(Locale.ROOT), "vector")) {
            return type.integerOrNull("dimension");
        } else {
            return null;
        }
    }

    private static String itemType(SchemaMap type) {
        SchemaMap receiver = type.mapOrNull("items");
        return DataModelV3GraphSpecMigration.scalarType(receiver == null ? null : receiver.stringOrNull("type"));
    }

    private static String scalarType(String string) {
        String lower = string == null ? null : string.toLowerCase(Locale.ROOT);
        if (lower == null) {
            return null;
        } else if (Objects.equals(lower, "localdatetime")) {
            return "LOCAL DATETIME";
        } else if (Objects.equals(lower, "datetime")) {
            return "ZONED DATETIME";
        } else if (Objects.equals(lower, "localtime")) {
            return "LOCAL TIME";
        } else if (Objects.equals(lower, "time")) {
            return "ZONED TIME";
        } else {
            return lower.toUpperCase(Locale.ROOT);
        }
    }

    private static final class ElementsByEntity {
        private final Map<String, List<SchemaMap>> nodes;

        private final Map<String, List<SchemaMap>> relationships;

        private ElementsByEntity(Map<String, List<SchemaMap>> nodes, Map<String, List<SchemaMap>> relationships) {
            this.nodes = nodes;
            this.relationships = relationships;
        }
    }
}
