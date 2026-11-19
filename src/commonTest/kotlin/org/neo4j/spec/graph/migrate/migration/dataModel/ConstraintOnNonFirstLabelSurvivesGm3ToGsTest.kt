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

class ConstraintOnNonFirstLabelSurvivesGm3ToGsTest {

    private val gsToGm3 = GraphSpecDataModelV3Migration()
    private val gm3ToGs = DataModelV3GraphSpecMigration()

    @Test
    fun `constraint on non-first label survives gm3 to gs to gm3 round-trip`() {
        val gm3Input = schemaMapOf(
            "version" to "3.0.0",
            "graphSchemaRepresentation" to schemaMapOf(
                "version" to "1.0.0",
                "graphSchema" to schemaMapOf(
                    "nodeLabels" to listOf(
                        schemaMapOf(
                            "\$id" to "nl:0",
                            "token" to "Person",
                            "properties" to listOf(
                                schemaMapOf("\$id" to "p:0", "token" to "ssn", "type" to schemaMapOf("type" to "string"))
                            )
                        ),
                        schemaMapOf(
                            "\$id" to "nl:1",
                            "token" to "Citizen",
                            "properties" to emptyList<Any>()
                        )
                    ),
                    "nodeObjectTypes" to listOf(
                        schemaMapOf(
                            "\$id" to "no:0",
                            "labels" to listOf(
                                schemaMapOf("\$ref" to "#nl:0"),
                                schemaMapOf("\$ref" to "#nl:1")
                            )
                        )
                    ),
                    "relationshipTypes" to emptyList<Any>(),
                    "relationshipObjectTypes" to emptyList<Any>(),
                    "constraints" to listOf(
                        schemaMapOf(
                            "\$id" to "c:0",
                            "name" to "citizen_ssn_uniq",
                            "constraintType" to "uniqueness",
                            "entityType" to "node",
                            "nodeLabel" to schemaMapOf("\$ref" to "#nl:1"),
                            "properties" to listOf(
                                schemaMapOf("\$ref" to "#p:0")
                            ),
                            "relationshipType" to null
                        )
                    ),
                    "indexes" to emptyList<Any>()
                )
            ),
            "graphSchemaExtensionsRepresentation" to schemaMapOf(),
            "graphMappingRepresentation" to schemaMapOf(
                "dataSourceSchema" to schemaMapOf("type" to "cloud", "tableSchemas" to emptyList<Any>()),
                "nodeMappings" to emptyList<Any>(),
                "relationshipMappings" to emptyList<Any>()
            ),
            "configurations" to schemaMapOf("idsToIgnore" to emptyList<String>())
        )

        val gm3RoundTripped = gm3Input.let(gm3ToGs::migrate).let(gsToGm3::migrate)

        assertEquals(
            gm3Input.map("graphSchemaRepresentation").map("graphSchema").list("constraints"),
            gm3RoundTripped.map("graphSchemaRepresentation").map("graphSchema").list("constraints"),
            "constraint on non-first label nl:1 dropped on gm3-gs-gm3 round-trip via DataModelV3GraphSpecMigration.kt:142 (labelRefs.firstOrNull)",
        )
    }
}
