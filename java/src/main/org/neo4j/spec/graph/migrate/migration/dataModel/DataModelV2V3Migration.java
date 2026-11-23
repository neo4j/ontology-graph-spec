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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.codec.schema.SchemaPrimitive;
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
        List<SchemaMap> nodeKeyProperties =
                schema.map("graphSchemaExtensionsRepresentation").listOfMaps("nodeKeyProperties");
        Map<String, String> references = new LinkedHashMap<>();
        for (var property : nodeKeyProperties) {
            SchemaMap key = property.removeMap("keyProperty");
            List<SchemaMap> keyProperties = new ArrayList<>();
            keyProperties.add(key);
            property.set("keyProperties", keyProperties);
            String nodeRef = DataModelUtils.ref(property, "node");
            references.put(nodeRef, DataModelUtils.ref(key));
        }
        Map<String, SchemaMap> relationshipObjectTypes = new LinkedHashMap<>();
        for (SchemaMap objectType :
                schema.map("graphSchemaRepresentation").map("graphSchema").listOfMaps("relationshipObjectTypes")) {
            relationshipObjectTypes.put(DataModelUtils.id(objectType), objectType);
        }
        for (var mapping : schema.map("graphMappingRepresentation").listOfMaps("relationshipMappings")) {
            String relationshipRef = DataModelUtils.ref(mapping, "relationship");
            SchemaMap objectType = relationshipObjectTypes.get(relationshipRef);
            if (objectType == null) {
                throw new IllegalStateException("Could not find relationship object " + relationshipRef);
            }
            String fromNodeRef = DataModelUtils.ref(objectType, "from");
            String toNodeRef = DataModelUtils.ref(objectType, "to");
            updateMapping(references, mapping, fromNodeRef, "fromMapping");
            updateMapping(references, mapping, toNodeRef, "toMapping");
        }
        return schema;
    }

    private void updateMapping(Map<String, String> references, SchemaMap mapping, String nodeRef, String key) {
        String toPropertyRef = references.get(nodeRef);
        if (toPropertyRef == null) {
            throw new IllegalStateException("Unable to resolve nodeKeyProperties to node ref " + nodeRef);
        }
        SchemaPrimitive toFieldName = mapping.map(key).literal("fieldName");
        mapping.set(key + "s", DataModelUtils.schemaMapOf(toPropertyRef, toFieldName));
        mapping.remove(key);
    }
}
