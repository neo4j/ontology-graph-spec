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

import org.neo4j.spec.graph.codec.schema.schemaMapOf
import kotlin.test.Test
import kotlin.test.assertEquals

class TableFieldTypeRichnessLossRoundTripTest {

    private val gsToGm3 = GraphSpecDataModelV3Migration()
    private val gm3ToGs = DataModelV3GraphSpecMigration()

    @Test
    fun `gm3 to gs to gm3 round-trip preserves isUsed flag on table fields`() {
        val gm3Input = schemaMapOf(
            "version" to "3.0.0",
            "graphSchemaRepresentation" to schemaMapOf(
                "version" to "1.0.0",
                "graphSchema" to schemaMapOf(
                    "nodeLabels" to emptyList<Any>(),
                    "nodeObjectTypes" to emptyList<Any>(),
                    "relationshipTypes" to emptyList<Any>(),
                    "relationshipObjectTypes" to emptyList<Any>(),
                    "constraints" to emptyList<Any>(),
                    "indexes" to emptyList<Any>()
                )
            ),
            "graphSchemaExtensionsRepresentation" to schemaMapOf(),
            "graphMappingRepresentation" to schemaMapOf(
                "dataSourceSchema" to schemaMapOf(
                    "type" to "cloud",
                    "tableSchemas" to listOf(
                        schemaMapOf(
                            "name" to "users",
                            "fields" to listOf(
                                schemaMapOf(
                                    "name" to "id",
                                    "rawType" to "INT",
                                    "recommendedType" to mapOf("type" to "integer"),
                                    "isUsed" to false,
                                    "sample" to "42"
                                )
                            )
                        )
                    )
                ),
                "nodeMappings" to emptyList<Any>(),
                "relationshipMappings" to emptyList<Any>()
            ),
            "configurations" to schemaMapOf("idsToIgnore" to emptyList<String>())
        )

        val gm3RoundTripped = gm3Input.let(gm3ToGs::migrate).let(gsToGm3::migrate)
        val fields = gm3RoundTripped
            .map("graphMappingRepresentation")
            .map("dataSourceSchema")
            .listOfMaps("tableSchemas")
            .first()
            .listOfMaps("fields")
        val idField = fields.first()

        assertEquals(
            false,
            idField.boolOrNull("isUsed"),
            "isUsed flag dropped on gm3-gs-gm3 round-trip: gm3ToGs omits it at DataModelV3GraphSpecMigration.kt:369-385 (migrateColumns), gsToGm3 restores no slot at GraphSpecDataModelV3Migration.kt:404-422 (convertFields)",
        )
    }
}
