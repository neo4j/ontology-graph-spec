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
package org.neo4j.spec.graph.migrate.migration.dataModel

import org.neo4j.spec.graph.codec.schema.SchemaMap
import org.neo4j.spec.graph.codec.schema.schemaMapOf
import org.neo4j.spec.graph.model.Version

/**
 * The Data Importer's model file, which envelopes the data model along with ui information.
 * The envelope `version` is independent of the data model's, migrations are versioned by the inner `dataModel.version`.
 */
internal object DataModelWrapper {
    const val DATA_MODEL = "dataModel"

    private val FIELDS = listOf("visualisation", "description")

    fun isWrapped(schema: SchemaMap) = schema.containsKey(DATA_MODEL)

    /**
     * Extracts the data model, carrying the envelope fields along as top-level keys
     */
    fun unwrap(schema: SchemaMap): SchemaMap {
        if (!isWrapped(schema)) {
            return schema
        }
        val model = schema.map(DATA_MODEL)
        schema.remove(DATA_MODEL)
        schema.remove("version")
        moveFields(schema, model)
        return model
    }

    /**
     * Envelopes a flat data model, moving the envelope fields out of it
     */
    fun wrap(schema: SchemaMap): SchemaMap {
        if (isWrapped(schema)) {
            return schema
        }
        val wrapped = schemaMapOf("version" to Version.DATA_MODEL_WRAPPED_V3_0)
        wrapped[DATA_MODEL] = schema
        moveFields(schema, wrapped)
        return wrapped
    }

    private fun moveFields(from: SchemaMap, to: SchemaMap) {
        for (field in FIELDS) {
            to[field] = from.remove(field) ?: continue
        }
    }
}
