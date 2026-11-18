// Generated from org/neo4j/spec/graph/migrate/migration/dataModel/DataModelV3GraphSpecMigration.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.migrate.migration.dataModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.neo4j.spec.graph.codec.schema.SchemaElement;
import org.neo4j.spec.graph.codec.schema.SchemaElements;
import org.neo4j.spec.graph.codec.schema.SchemaLiteral;
import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.codec.schema.SchemaPrimitive;
import org.neo4j.spec.graph.internal.Kt;
import org.neo4j.spec.graph.internal.Pair;
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
        SchemaMap receiver = schema.mapOrNull("graphSchemaRepresentation");
        SchemaMap graphSchema = receiver == null ? null : receiver.mapOrNull("graphSchema");
        if (graphSchema == null) {
            return SchemaElements.schemaMapOf(new Pair<>("schema", Version.schemaUrl(Version.V1_1)), new Pair<>("version", "1.0.0"), new Pair<>("id", "graph-model"), new Pair<>("description", schema.literalOrNull("description")), new Pair<>("nodes", Kt.<String, SchemaMap>emptyMap()), new Pair<>("relationships", Kt.<String, SchemaMap>emptyMap()), SchemaElements.toNotEmpty("extensions", extensions(migrateTables(schema), Kt.<SchemaMap>emptyList())));
        }
        var tmp = gatherWithNames(graphSchema, "constraints");
        var nodeConstraints = tmp.getFirst();
        var relationshipConstraints = tmp.getSecond();
        var tmp2 = gatherWithNames(graphSchema, "indexes");
        var nodeIndexes = tmp2.getFirst();
        var relationshipIndexes = tmp2.getSecond();
        Map<String, SchemaMap> nodes = migrateNodes(graphSchema, nodeConstraints, nodeIndexes);
        convertVisualisations(schema, nodes);
        return SchemaElements.schemaMapOf(new Pair<>("schema", Version.schemaUrl(Version.V1_1)), new Pair<>("version", "1.0.0"), new Pair<>("id", "graph-model"), new Pair<>("description", schema.literalOrNull("description")), new Pair<>("nodes", nodes), new Pair<>("relationships", migrateRelationships(graphSchema, relationshipConstraints, relationshipIndexes)), SchemaElements.toNotEmpty("extensions", extensions(migrateTables(schema), Kt.plusAll(nodeMappings(schema, nodeKeys), relationshipMappings(schema, relKeys)))));
    }

    /**
     * Graph model level extensions, omitting any which are empty.
     */
    private Map<String, Object> extensions(Map<String, SchemaMap> tables,
            List<SchemaMap> mappings) {
        Map<String, Object> extensions = new LinkedHashMap<String, Object>();
        if (!tables.isEmpty()) {
            extensions.put("data_source_tables", tables);
        }
        if (!mappings.isEmpty()) {
            extensions.put("data_source_mappings", Kt.mapOf(new Pair<>(DataModelV3GraphSpecMigration.MAPPING_ID, mappings)));
        }
        return extensions;
    }

    private Map<String, Set<String>> keyProperties(SchemaMap extensions, String entity) {
        List<SchemaMap> receiver = extensions == null ? null : extensions.listOfMapsOrNull(entity + "KeyProperties");
        Map<String, Set<String>> value = receiver == null ? null : Kt.associate(receiver, kp -> new Pair<>(DataModelUtilsKt.ref(kp, entity), Kt.toSet(Kt.map(kp.listOfMaps("keyProperties"), it -> DataModelUtilsKt.ref(it)))));
        return value != null ? value : Kt.<String, Set<String>>emptyMap();
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
            String ref = Kt.removePrefix(vis.string("id"), "#");
            SchemaMap node = nodes.get(ref);
            if (node == null) {
                throw new IllegalStateException("Unknown node " + ref);
            }
            SchemaMap position = vis.map("position");
            SchemaMap extensions = node.mapOrNull("extensions");
            if (extensions == null) {
                SchemaMap it = SchemaElements.schemaMapOf();
                node.set("extensions", it);
                extensions = it;
            }
            extensions.set("display", SchemaElements.schemaMapOf(new Pair<>("x", position.literal("x")), new Pair<>("y", position.literal("y"))));
        }
    }

    public Pair<Map<String, List<SchemaMap>>, Map<String, List<SchemaMap>>> gatherWithNames(
            SchemaMap schema, String key) {
        List<SchemaMap> elements = schema.listOfMapsOrNull(key);
        if (elements == null) {
            return new Pair<>(Kt.<String, List<SchemaMap>>emptyMap(), Kt.<String, List<SchemaMap>>emptyMap());
        }
        var tmp = Kt.partition(elements, it -> Objects.equals(it.string("entityType"), "node"));
        var nodes = tmp.getFirst();
        var rels = tmp.getSecond();
        Map<String, List<SchemaMap>> nodeElements = Kt.groupBy(nodes, it -> DataModelUtilsKt.ref(it, "nodeLabel"));
        Map<String, List<SchemaMap>> relationshipElements = Kt.groupBy(rels, it -> DataModelUtilsKt.ref(it, "relationshipType"));
        for (var element : elements) {
            element.set("name", element.stringOrNull("name"));
        }
        return new Pair<>(nodeElements, relationshipElements);
    }

    public Map<String, SchemaMap> migrateNodes(SchemaMap schema,
            Map<String, ? extends List<SchemaMap>> constraints,
            Map<String, ? extends List<SchemaMap>> indexes) {
        Map<String, SchemaMap> nodes = new LinkedHashMap<String, SchemaMap>();
        List<SchemaMap> receiver = schema.listOfMapsOrNull("nodeLabels");
        Map<String, SchemaMap> nodeLabels = receiver == null ? null : Kt.associateBy(receiver, it -> DataModelUtilsKt.id(it));
        if (nodeLabels == null) {
            return nodes;
        }
        List<SchemaMap> nodeObjTypes = schema.listOfMapsOrNull("nodeObjectTypes");
        if (nodeObjTypes == null) {
            return nodes;
        }
        for (var nodeObject : nodeObjTypes) {
            List<String> labelRefs = Kt.map(nodeObject.listOfMaps("labels"), it -> DataModelUtilsKt.ref(it));
            List<SchemaMap> labels = Kt.map(labelRefs, labelRef -> {
                SchemaMap value = nodeLabels.get(labelRef);
                if (value == null) {
                    throw new IllegalStateException("Label " + labelRef + " not found");
                }
                return value;
            });
            List<String> tokens = Kt.map(labels, it -> it.string("token"));
            String labelRef = Kt.firstOrNull(labelRefs);
            String primaryLabel = Kt.first(tokens);
            Map<String, String> propertyTokens = Kt.associate(Kt.flatMap(labels, it -> Kt.orEmpty(it.listOfMapsOrNull("properties"))), it -> new Pair<>(DataModelUtilsKt.id(it), it.string("token")));
            String id = DataModelUtilsKt.id(nodeObject);
            nodes.put(id, SchemaElements.schemaMapOf(new Pair<>("labels", SchemaElements.schemaMapOf(new Pair<>("identifier", primaryLabel), SchemaElements.toNotEmpty("implied", Kt.drop(tokens, 1)))), SchemaElements.toNotEmpty("constraints", convertConstraints(((Map<String, List<SchemaMap>>) constraints), labelRef, primaryLabel, propertyTokens)), SchemaElements.toNotEmpty("properties", convertProperties(labels)), SchemaElements.toNotEmpty("extensions", Kt.mapValues(Kt.filterValues(Kt.mapOf(new Pair<>("indexes", convertIndexes(((Map<String, List<SchemaMap>>) indexes), labelRef, primaryLabel, propertyTokens))), it -> !Kt.isNullOrEmpty(it)), it -> Objects.requireNonNull(it.getValue()))), new Pair<>("description", nodeObject.literalOrNull("description"))));
        }
        return nodes;
    }

    public Map<String, SchemaMap> convertIndexes(Map<String, ? extends List<SchemaMap>> indexes,
            String labelRef, String label, Map<String, String> propertyTokens) {
        List<SchemaMap> receiver = ((Map<String, List<SchemaMap>>) indexes).get(labelRef);
        return receiver == null ? null : Kt.associate(receiver, index -> {
            String id = DataModelUtilsKt.id(index);
            IndexType resolvedType = indexType(index);
            List<SchemaMap> properties = index.listOfMapsOrNull("properties");
            SchemaMap receiver2 = index.mapOrNull("options");
            SchemaMap result = null;
            if (receiver2 != null) {
                receiver2.set("type", resolvedType.name());
                result = receiver2;
            }
            String receiver3 = index.stringOrNull("name");
            String value = receiver3 == null ? null : !Kt.isBlank(receiver3) ? receiver3 : null;
            return new Pair<>(id, SchemaElements.schemaMapOf(new Pair<>("type", resolvedType.name()), new Pair<>("labels", Kt.listOf(label)), new Pair<>("properties", properties == null ? null : Kt.map(properties, it -> DataModelUtilsKt.ref(it))), SchemaElements.toNotEmpty("options", result), new Pair<>("name", value != null ? value : NameFormat.indexName(tokensFrom(properties, propertyTokens), label, resolvedType))));
        });
    }

    private IndexType indexType(SchemaMap index) {
        String type = index.string("indexType");
        IndexType value = DataModelUtilsKt.dataModelIndexTypeFrom(type);
        if (value == null) {
            throw new IllegalStateException("Unknown index type: '" + type + "' at " + index.getPath() + "." + index.string("name"));
        }
        return value;
    }

    public Map<String, SchemaMap> convertConstraints(
            Map<String, ? extends List<SchemaMap>> constraints, String labelRef, String label,
            Map<String, String> propertyTokens) {
        List<SchemaMap> receiver = ((Map<String, List<SchemaMap>>) constraints).get(labelRef);
        return receiver == null ? null : Kt.associate(receiver, constraint -> {
            List<SchemaMap> properties = constraint.listOfMapsOrNull("properties");
            ConstraintType constraintType = constraintType(constraint);
            if (properties != null && properties.size() > 1 && constraintType == ConstraintType.PROPERTY_TYPE) {
                throw new IllegalStateException("Type constraints not supported on multiple properties.");
            }
            String id = DataModelUtilsKt.id(constraint);
            String receiver2 = constraint.stringOrNull("name");
            String value = receiver2 == null ? null : !Kt.isBlank(receiver2) ? receiver2 : null;
            return new Pair<>(id, SchemaElements.schemaMapOf(new Pair<>("type", constraintType.name()), SchemaElements.toNotEmpty("properties", properties == null ? null : Kt.map(properties, it -> DataModelUtilsKt.ref(it))), new Pair<>("name", value != null ? value : generatedName(properties, propertyTokens, label, constraintType))));
        });
    }

    private String generatedName(List<SchemaMap> properties, Map<String, String> propertyTokens,
            String label, ConstraintType constraintType) {
        return NameFormat.constraintName(tokensFrom(properties, propertyTokens), label, constraintType);
    }

    private List<String> tokensFrom(List<SchemaMap> receiver, Map<String, String> propertyTokens) {
        return Kt.map(Kt.orEmpty(receiver), it -> {
            String value = propertyTokens.get(DataModelUtilsKt.ref(it));
            return value != null ? value : DataModelUtilsKt.ref(it);
        });
    }

    private ConstraintType constraintType(SchemaMap constraint) {
        String type = constraint.string("constraintType");
        ConstraintType value = DataModelUtilsKt.dataModelConstraintTypeFrom(type);
        if (value == null) {
            throw new IllegalStateException("Unknown constraint type: " + type + " at " + constraint.getPath() + "." + constraint.string("name"));
        }
        return value;
    }

    public Map<String, SchemaMap> migrateRelationships(SchemaMap schema,
            Map<String, ? extends List<SchemaMap>> constraints,
            Map<String, ? extends List<SchemaMap>> indexes) {
        Map<String, SchemaMap> relationships = new LinkedHashMap<String, SchemaMap>();
        List<SchemaMap> receiver = schema.listOfMapsOrNull("relationshipTypes");
        Map<String, SchemaMap> relationshipTypes = receiver == null ? null : Kt.associateBy(receiver, it -> DataModelUtilsKt.id(it));
        if (relationshipTypes == null) {
            return relationships;
        }
        List<SchemaMap> relObjTypes = schema.listOfMapsOrNull("relationshipObjectTypes");
        if (relObjTypes == null) {
            return relationships;
        }
        for (var objectType : relObjTypes) {
            String typeRef = DataModelUtilsKt.ref(objectType, "type");
            SchemaMap relationshipType = relationshipTypes.get(typeRef);
            if (relationshipType == null) {
                throw new IllegalStateException("RelationshipType " + typeRef + " not found");
            }
            String token = relationshipType.string("token");
            Map<String, String> propertyTokens = Kt.associate(Kt.orEmpty(relationshipType.listOfMapsOrNull("properties")), it -> new Pair<>(DataModelUtilsKt.id(it), it.string("token")));
            String id = DataModelUtilsKt.id(objectType);
            relationships.put(id, SchemaElements.schemaMapOf(new Pair<>("type", token), new Pair<>("from", Kt.mapOf(new Pair<>("node", DataModelUtilsKt.ref(objectType, "from")))), new Pair<>("to", Kt.mapOf(new Pair<>("node", DataModelUtilsKt.ref(objectType, "to")))), new Pair<>("properties", convertProperties(Kt.listOf(relationshipType))), SchemaElements.toNotEmpty("constraints", convertConstraints(((Map<String, List<SchemaMap>>) constraints), typeRef, token, propertyTokens)), SchemaElements.toNotEmpty("extensions", Kt.mapValues(Kt.filterValues(Kt.mapOf(new Pair<>("indexes", convertIndexes(((Map<String, List<SchemaMap>>) indexes), typeRef, token, propertyTokens))), it -> !Kt.isNullOrEmpty(it)), it -> Objects.requireNonNull(it.getValue()))), new Pair<>("description", objectType.literalOrNull("description"))));
        }
        return relationships;
    }

    public Map<String, SchemaMap> convertProperties(List<SchemaMap> labels) {
        return Kt.associate(Kt.flatMap(labels, label -> label.listOfMaps("properties")), property -> {
            SchemaMap typeObj = property.map("type");
            SchemaMap map = SchemaElements.schemaMapOf(new Pair<>("name", property.literalOrNull("token")), new Pair<>("type", DataModelV3GraphSpecMigration.neo4jType(typeObj)), new Pair<>("dimension", DataModelV3GraphSpecMigration.dimension(typeObj)), new Pair<>("description", property.literalOrNull("description")));
            return new Pair<>(DataModelUtilsKt.id(property), map);
        });
    }

    public List<SchemaMap> relationshipMappings(SchemaMap schema,
            Map<String, ? extends Set<String>> relKeys) {
        SchemaMap graph = schema.map("graphSchemaRepresentation").map("graphSchema");
        List<SchemaMap> receiver = graph.listOfMapsOrNull("relationshipObjectTypes");
        Map<String, SchemaMap> relInfo = receiver == null ? null : Kt.associateBy(receiver, it -> DataModelUtilsKt.id(it));
        if (relInfo == null) {
            return Kt.<SchemaMap>emptyList();
        }
        List<SchemaMap> relationshipMappings = schema.map("graphMappingRepresentation").listOfMapsOrNull("relationshipMappings");
        if (relationshipMappings == null) {
            return Kt.<SchemaMap>emptyList();
        }
        List<SchemaMap> mappings = new ArrayList<SchemaMap>();
        for (var mapping : relationshipMappings) {
            String ref = DataModelUtilsKt.ref(mapping, "relationship");
            if (!relInfo.containsKey(ref)) {
                throw new IllegalStateException("Relationship " + ref + " not found");
            }
            Set<String> value = ((Map<String, Set<String>>) relKeys).get(ref);
            mappings.add(SchemaElements.schemaMapOf(new Pair<>("type", new SchemaLiteral(MappingType.RELATIONSHIP)), new Pair<>("relationship", ref), new Pair<>("from_node", Kt.mapOf(new Pair<>("properties", entityMap(mapping, "fromMappings")))), new Pair<>("to_node", Kt.mapOf(new Pair<>("properties", entityMap(mapping, "toMappings")))), new Pair<>("table", mapping.literal("tableName")), SchemaElements.toNotEmpty("properties", migratePropertyMappings(mapping)), SchemaElements.toNotEmpty("key", migrateKeyProperties(mapping, value != null ? value : Kt.<String>emptySet()))));
        }
        return mappings;
    }

    public Map<String, SchemaMap> entityMap(SchemaMap schemaMap, String key) {
        SchemaMap receiver = schemaMap.mapOrNull(key);
        List<Pair<String, SchemaMap>> receiver2 = receiver == null ? null : Kt.map(receiver, entry -> {
            var key2 = entry.getKey();
            var value = entry.getValue();
            return new Pair<>(Kt.removePrefix(key2, "#"), SchemaElements.schemaMapOf(new Pair<>("column", value)));
        });
        Map<String, SchemaMap> value = receiver2 == null ? null : Kt.toMap(receiver2);
        return value != null ? value : Kt.<String, SchemaMap>emptyMap();
    }

    public List<SchemaMap> nodeMappings(SchemaMap schema,
            Map<String, ? extends Set<String>> nodeKeys) {
        List<SchemaMap> nodeMappings = schema.map("graphMappingRepresentation").listOfMapsOrNull("nodeMappings");
        if (nodeMappings == null) {
            return Kt.<SchemaMap>emptyList();
        }
        List<SchemaMap> mappings = new ArrayList<SchemaMap>();
        for (var nodeMapping : nodeMappings) {
            String ref = DataModelUtilsKt.ref(nodeMapping, "node");
            Set<String> value = ((Map<String, Set<String>>) nodeKeys).get(ref);
            mappings.add(SchemaElements.schemaMapOf(new Pair<>("type", new SchemaLiteral(MappingType.NODE)), new Pair<>("node", ref), new Pair<>("table", nodeMapping.literal("tableName")), SchemaElements.toNotEmpty("properties", migratePropertyMappings(nodeMapping)), SchemaElements.toNotEmpty("key", migrateKeyProperties(nodeMapping, value != null ? value : Kt.<String>emptySet()))));
        }
        return mappings;
    }

    private Map<String, Map<String, SchemaPrimitive>> migratePropertyMappings(SchemaMap entity) {
        return Kt.associate(entity.listOfMaps("propertyMappings"), mapping -> new Pair<>(DataModelUtilsKt.ref(mapping, "property"), Kt.mapOf(new Pair<>("column", mapping.literal("fieldName")))));
    }

    private List<String> migrateKeyProperties(SchemaMap entity, Set<String> keys) {
        return Kt.mapNotNull(entity.listOfMaps("propertyMappings"), mapping -> {
            String ref = DataModelUtilsKt.ref(mapping, "property");
            if (keys.contains(ref)) {
                return ref;
            } else {
                return null;
            }
        });
    }

    public Map<String, SchemaMap> migrateTables(SchemaMap schema) {
        Map<String, SchemaMap> tables = new LinkedHashMap<String, SchemaMap>();
        SchemaMap sourceSchema = schema.map("graphMappingRepresentation").mapOrNull("dataSourceSchema");
        if (sourceSchema == null) {
            return tables;
        }
        for (var table : sourceSchema.listOfMaps("tableSchemas")) {
            String name = table.string("name");
            tables.put(name, SchemaElements.schemaMapOf(new Pair<>("source", sourceSchema.literalOrNull("type")), new Pair<>("columns", migrateColumns(table)), SchemaElements.toNotEmpty("primary_keys", table.listOrNull("primaryKeys")), SchemaElements.toNotEmpty("foreign_keys", migrateForeignKeys(table))));
        }
        return tables;
    }

    private Map<String, SchemaElement> migrateForeignKeys(SchemaMap table) {
        List<SchemaMap> foreign = table.listOfMapsOrNull("foreignKeys");
        if (foreign == null) {
            return null;
        }
        Map<String, SchemaElement> foreignKeys = new LinkedHashMap<String, SchemaElement>();
        for (var foreignKey : foreign) {
            List<String> columns = Kt.map(foreignKey.listOfMaps("fields"), it -> it.string("field"));
            List<String> referencedColumns = Kt.map(foreignKey.listOfMaps("fields"), it -> it.string("referencedField"));
            foreignKeys.put("foreignKey" + (foreignKeys.size() + 1), SchemaElements.schemaMapOf(new Pair<>("columns", columns), new Pair<>("references", Kt.mapOf(new Pair<>("table", foreignKey.literal("referencedTable")), new Pair<>("columns", referencedColumns)))));
        }
        return foreignKeys;
    }

    private List<SchemaMap> migrateColumns(SchemaMap table) {
        return Kt.map(table.listOfMaps("fields"), field -> {
            List<SchemaMap> receiver = field.listOfMapsOrNull("supportedTypes");
            Integer value = DataModelV3GraphSpecMigration.dimension(field.mapOrNull("recommendedType"));
            if (value == null) {
                List<SchemaMap> receiver2 = field.listOfMapsOrNull("supportedTypes");
                value = receiver2 == null ? null : Kt.firstNotNullOfOrNull(receiver2, it -> DataModelV3GraphSpecMigration.dimension(it));
            }
            SchemaLiteral receiver2 = field.literalOrNull("sample");
            return SchemaElements.schemaMapOf(new Pair<>("name", field.literal("name")), new Pair<>("type", field.literalOrNull("rawType")), new Pair<>("size", field.literalOrNull("size")), new Pair<>("suggested", DataModelV3GraphSpecMigration.neo4jType(field.mapOrNull("recommendedType"))), new Pair<>("supported", receiver == null ? null : Kt.map(receiver, it -> DataModelV3GraphSpecMigration.neo4jType(it))), new Pair<>("dimension", value), SchemaElements.toNotEmpty("custom", receiver2 == null ? null : Kt.mapOf(new Pair<>("sample", receiver2))));
        });
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
}
