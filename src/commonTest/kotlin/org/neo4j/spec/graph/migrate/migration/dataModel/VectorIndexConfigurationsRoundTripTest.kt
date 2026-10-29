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

class VectorIndexConfigurationsRoundTripTest {

    private val gsToGm3 = GraphSpecDataModelV3Migration()
    private val gm3ToGs = DataModelV3GraphSpecMigration()

    @Test
    fun `gm3 to gs to gm3 round-trip preserves vector index configurations`() {
        val gm3Input = schemaMapOf(
            "version" to "3.0.0",
            "graphSchemaRepresentation" to schemaMapOf(
                "version" to "1.0.0",
                "graphSchema" to schemaMapOf(
                    "nodeLabels" to listOf(
                        schemaMapOf(
                            "\$id" to "nl:0",
                            "token" to "Document",
                            "properties" to listOf(
                                schemaMapOf("\$id" to "p1", "token" to "embedding", "type" to mapOf("type" to "string"))
                            )
                        )
                    ),
                    "nodeObjectTypes" to emptyList<Any>(),
                    "relationshipTypes" to emptyList<Any>(),
                    "relationshipObjectTypes" to emptyList<Any>(),
                    "constraints" to emptyList<Any>(),
                    "indexes" to emptyList<Any>()
                )
            ),
            "graphSchemaExtensionsRepresentation" to schemaMapOf(
                "vectorIndexConfigurations" to listOf(
                    schemaMapOf(
                        "index" to mapOf("\$ref" to "#Document.embedding"),
                        "dimensions" to 768,
                        "similarityFunction" to "cosine",
                        "quantizationEnabled" to true,
                        "hnswM" to 16,
                        "hnswEfConstruction" to 200
                    )
                )
            ),
            "graphMappingRepresentation" to schemaMapOf(
                "dataSourceSchema" to schemaMapOf("type" to "local", "tableSchemas" to emptyList<Any>()),
                "nodeMappings" to emptyList<Any>(),
                "relationshipMappings" to emptyList<Any>()
            ),
            "configurations" to schemaMapOf("idsToIgnore" to emptyList<String>())
        )

        val gm3RoundTripped = gm3Input.let(gm3ToGs::migrate).let(gsToGm3::migrate)
        val vectorConfigs = assertNotNull(
            gm3RoundTripped.map("graphSchemaExtensionsRepresentation").listOfMapsOrNull("vectorIndexConfigurations"),
            "vector index configurations section absent from gm3-gs-gm3 round-trip output",
        )

        assertEquals(
            1,
            vectorConfigs.size,
            "vector index configurations count reduced on gm3-gs-gm3 round-trip",
        )
        val first = vectorConfigs.first()
        assertEquals(
            768,
            assertNotNull(first.intOrNull("dimensions"), "vector index dimensions slot lost on gm3-gs-gm3 round-trip"),
        )
        assertEquals(
            "cosine",
            assertNotNull(
                first.stringOrNull("similarityFunction"),
                "vector index similarityFunction slot lost on gm3-gs-gm3 round-trip",
            ),
        )
        assertEquals(
            true,
            assertNotNull(
                first.boolOrNull("quantizationEnabled"),
                "vector index quantizationEnabled slot lost on gm3-gs-gm3 round-trip",
            ),
        )
    }
}
