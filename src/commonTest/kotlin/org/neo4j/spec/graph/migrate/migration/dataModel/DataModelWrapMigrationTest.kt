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

import org.neo4j.spec.graph.GraphSpec
import org.neo4j.spec.graph.codec.schema.schemaMapOf
import org.neo4j.spec.graph.migrate.MigrationPath
import org.neo4j.spec.graph.model.Type
import org.neo4j.spec.graph.model.Version
import kotlin.test.Test
import kotlin.test.assertEquals

class DataModelWrapMigrationTest {

    private val migrations = MigrationPath(GraphSpec.Json.configuration.migrations)

    @Test
    fun `graph spec migrates directly to the wrapped data model`() {
        val path = migrations.findPath(
            "${Type.GRAPH_SPEC}:${Version.LATEST}",
            "${Type.DATA_MODEL_WRAPPED}:${Version.DATA_MODEL_V3_0}"
        )

        assertEquals(listOf(GraphSpecDataModelWrappedMigration::class), path?.map { it::class })
    }

    @Test
    fun `wrapped data models upgrade through the flat migrations`() {
        val path = migrations.findPath(
            "${Type.DATA_MODEL_WRAPPED}:${Version.DATA_MODEL_V2_4}",
            "${Type.DATA_MODEL_WRAPPED}:${Version.DATA_MODEL_V3_0}"
        )

        assertEquals(
            listOf(DataModelUnwrapMigration::class, DataModelV2V3Migration::class, DataModelWrapMigration::class),
            path?.map { it::class }
        )
    }

    @Test
    fun `unwrap then wrap keeps the envelope fields`() {
        val wrapped = schemaMapOf(
            "version" to "1.2.3",
            "dataModel" to schemaMapOf("version" to "3.0.0"),
            "visualisation" to schemaMapOf("nodes" to emptyList<Any>()),
            "description" to "a description"
        )

        val result = DataModelWrapMigration().migrate(DataModelUnwrapMigration(Version.DATA_MODEL_V3_0).migrate(wrapped))

        assertEquals(Version.DATA_MODEL_WRAPPED_V3_0, result.string("version"))
        assertEquals("a description", result.string("description"))
        assertEquals(setOf("version", "dataModel", "visualisation", "description"), result.keys)
    }
}
