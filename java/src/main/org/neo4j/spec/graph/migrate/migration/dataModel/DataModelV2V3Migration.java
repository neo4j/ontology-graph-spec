// Generated from org/neo4j/spec/graph/migrate/migration/dataModel/DataModelV2V3Migration.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.migrate.migration.dataModel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.spec.graph.codec.schema.SchemaElements;
import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.codec.schema.SchemaPrimitive;
import org.neo4j.spec.graph.internal.Kt;
import org.neo4j.spec.graph.internal.Pair;
import org.neo4j.spec.graph.migrate.Migration;
import org.neo4j.spec.graph.model.Type;
import org.neo4j.spec.graph.model.Version;

/**
 * 2.X -> 3.0 adds support for multiple keys properties
 */
public final class DataModelV2V3Migration extends Migration {
    public DataModelV2V3Migration(String version) {
        super(Type.DATA_MODEL, version, Type.DATA_MODEL, Version.DATA_MODEL_V3_0);
    }

    @Override
    public SchemaMap migrate(SchemaMap schema) {
        List<SchemaMap> nodeKeyProperties = schema.map("graphSchemaExtensionsRepresentation").listOfMaps("nodeKeyProperties");
        Map<String, String> references = new LinkedHashMap<String, String>();
        for (var property : nodeKeyProperties) {
            SchemaMap key = property.removeMap("keyProperty");
            property.set("keyProperties", Kt.mutableListOf(key));
            String nodeRef = DataModelUtilsKt.ref(property, "node");
            references.put(nodeRef, DataModelUtilsKt.ref(key));
        }
        Map<String, SchemaMap> relationshipObjectTypes = Kt.associateBy(schema.map("graphSchemaRepresentation").map("graphSchema").listOfMaps("relationshipObjectTypes"), it -> DataModelUtilsKt.id(it));
        for (var mapping : schema.map("graphMappingRepresentation").listOfMaps("relationshipMappings")) {
            String relationshipRef = DataModelUtilsKt.ref(mapping, "relationship");
            SchemaMap objectType = relationshipObjectTypes.get(relationshipRef);
            if (objectType == null) {
                throw new IllegalStateException("Could not find relationship object " + relationshipRef);
            }
            String fromNodeRef = DataModelUtilsKt.ref(objectType, "from");
            String toNodeRef = DataModelUtilsKt.ref(objectType, "to");
            updateMapping(references, mapping, fromNodeRef, "fromMapping");
            updateMapping(references, mapping, toNodeRef, "toMapping");
        }
        return schema;
    }

    private void updateMapping(Map<String, String> references, SchemaMap mapping, String nodeRef,
            String key) {
        String toPropertyRef = references.get(nodeRef);
        if (toPropertyRef == null) {
            throw new IllegalStateException("Unable to resolve nodeKeyProperties to node ref " + nodeRef);
        }
        SchemaPrimitive toFieldName = mapping.map(key).literal("fieldName");
        mapping.set(key + "s", SchemaElements.schemaMapOf(new Pair<>(toPropertyRef, toFieldName)));
        mapping.remove(key);
    }
}
