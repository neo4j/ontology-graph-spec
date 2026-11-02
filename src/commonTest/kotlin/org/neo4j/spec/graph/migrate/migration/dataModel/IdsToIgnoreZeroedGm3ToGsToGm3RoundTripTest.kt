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

import org.neo4j.spec.graph.codec.schema.schemaListOf
import org.neo4j.spec.graph.codec.schema.schemaMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IdsToIgnoreZeroedGm3ToGsToGm3RoundTripTest {

    private val gsToGm3 = GraphSpecDataModelV3Migration()
    private val gm3ToGs = DataModelV3GraphSpecMigration()

    @Test
    fun `gm3 to gs to gm3 round-trip preserves idsToIgnore non-empty values`() {
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
                "dataSourceSchema" to schemaMapOf("type" to "local", "tableSchemas" to emptyList<Any>()),
                "nodeMappings" to emptyList<Any>(),
                "relationshipMappings" to emptyList<Any>()
            ),
            "configurations" to schemaMapOf(
                "idsToIgnore" to schemaListOf("0", "1", "6")
            )
        )

        val gm3RoundTripped = gm3Input.let(gm3ToGs::migrate).let(gsToGm3::migrate)
        val idsToIgnore = assertNotNull(
            gm3RoundTripped.map("configurations").listOrNull("idsToIgnore"),
            "idsToIgnore section absent from configurations on gm3-gs-gm3 round-trip; DataModelV3GraphSpecMigration does not carry configurations through, GraphSpecDataModelV3Migration.kt:62 hardcodes an empty list"
        )

        assertEquals(
            schemaListOf("0", "1", "6"),
            idsToIgnore,
            "configurations.idsToIgnore zeroed on gm3-gs-gm3 round-trip (GraphSpecDataModelV3Migration.kt:62 always emits empty)"
        )
    }
}
