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
import kotlin.test.assertNotNull

class MappingFilterDroppedOnBothDirectionsRoundTripTest {

    private val gsToGm3 = GraphSpecDataModelV3Migration()
    private val gm3ToGs = DataModelV3GraphSpecMigration()

    @Test
    fun `gm3 to gs to gm3 round-trip preserves mappingFilter on node mapping`() {
        val filterInput = schemaMapOf(
            "fieldName" to "active",
            "exactMatch" to "true"
        )
        val gm3Input = schemaMapOf(
            "version" to "3.0.0",
            "graphSchemaRepresentation" to schemaMapOf(
                "version" to "1.0.0",
                "graphSchema" to schemaMapOf(
                    "nodeLabels" to listOf(
                        schemaMapOf("\$id" to "nl:0", "token" to "Person", "properties" to emptyList<Any>())
                    ),
                    "nodeObjectTypes" to listOf(
                        schemaMapOf("\$id" to "n1", "labels" to listOf(mapOf("\$ref" to "#nl:0")))
                    ),
                    "relationshipTypes" to emptyList<Any>(),
                    "relationshipObjectTypes" to emptyList<Any>(),
                    "constraints" to emptyList<Any>(),
                    "indexes" to emptyList<Any>()
                )
            ),
            "graphSchemaExtensionsRepresentation" to schemaMapOf(),
            "graphMappingRepresentation" to schemaMapOf(
                "dataSourceSchema" to schemaMapOf("type" to "local", "tableSchemas" to emptyList<Any>()),
                "nodeMappings" to listOf(
                    schemaMapOf(
                        "node" to mapOf("\$ref" to "#n1"),
                        "tableName" to "users",
                        "mappingFilter" to filterInput,
                        "propertyMappings" to emptyList<Any>()
                    )
                ),
                "relationshipMappings" to emptyList<Any>()
            ),
            "configurations" to schemaMapOf("idsToIgnore" to emptyList<String>())
        )

        val gm3RoundTripped = gm3Input.let(gm3ToGs::migrate).let(gsToGm3::migrate)
        val nodeMapping = assertNotNull(
            gm3RoundTripped
                .map("graphMappingRepresentation")
                .listOfMapsOrNull("nodeMappings")
                ?.firstOrNull(),
            "node mapping listing absent on gm3-gs-gm3 round-trip",
        )
        val filterActual = assertNotNull(
            nodeMapping.mapOrNull("mappingFilter"),
            "mappingFilter slot dropped on gm3-gs-gm3 round-trip: omitted at DataModelV3GraphSpecMigration.kt:311-317 (gm3ToGs nodeMappings) and never re-emitted at GraphSpecDataModelV3Migration.kt:71-75 (gsToGm3 convertGraphMapping node assembly)"
        )

        assertEquals(
            filterInput,
            filterActual,
            "mappingFilter contents not preserved on gm3-gs-gm3 round-trip (same UPX lines in both directions)"
        )
    }
}
