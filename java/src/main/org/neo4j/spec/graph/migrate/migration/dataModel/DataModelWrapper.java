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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.neo4j.spec.graph.codec.schema.SchemaElement;
import org.neo4j.spec.graph.codec.schema.SchemaMap;
import org.neo4j.spec.graph.model.Version;

/**
 * The Data Importer's model file, which envelopes the data model along with ui information.
 * The envelope `version` is independent of the data model's, migrations are versioned by the inner `dataModel.version`.
 */
public final class DataModelWrapper {
    public static final String DATA_MODEL = "dataModel";

    private static final List<String> FIELDS =
            Collections.unmodifiableList(Arrays.asList("visualisation", "description"));

    private DataModelWrapper() {}

    public static boolean isWrapped(SchemaMap schema) {
        return schema.containsKey(DataModelWrapper.DATA_MODEL);
    }

    /**
     * Extracts the data model, carrying the envelope fields along as top-level keys
     */
    public static SchemaMap unwrap(SchemaMap schema) {
        if (!DataModelWrapper.isWrapped(schema)) {
            return schema;
        }
        SchemaMap model = schema.map(DataModelWrapper.DATA_MODEL);
        schema.remove(DataModelWrapper.DATA_MODEL);
        schema.remove("version");
        DataModelWrapper.moveFields(schema, model);
        return model;
    }

    /**
     * Envelopes a flat data model, moving the envelope fields out of it
     */
    public static SchemaMap wrap(SchemaMap schema) {
        if (DataModelWrapper.isWrapped(schema)) {
            return schema;
        }
        SchemaMap wrapped = DataModelUtils.schemaMapOf("version", Version.DATA_MODEL_WRAPPED_V3_0);
        wrapped.set(DataModelWrapper.DATA_MODEL, schema);
        DataModelWrapper.moveFields(schema, wrapped);
        return wrapped;
    }

    private static void moveFields(SchemaMap from, SchemaMap to) {
        for (var field : DataModelWrapper.FIELDS) {
            SchemaElement value = from.remove(field);
            if (value == null) {
                continue;
            }
            to.set(field, value);
        }
    }
}
