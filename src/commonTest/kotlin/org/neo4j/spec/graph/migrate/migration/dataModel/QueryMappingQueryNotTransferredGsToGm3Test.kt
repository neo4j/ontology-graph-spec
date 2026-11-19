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

class QueryMappingQueryNotTransferredGsToGm3Test {

    private val gsToGm3 = GraphSpecDataModelV3Migration()
    private val gm3ToGs = DataModelV3GraphSpecMigration()

    @Test
    fun `gs to gm3 carries QueryMapping entries into graphSchemaExtensionsRepresentation`() {
        val gsInput = schemaMapOf(
            "version" to "1.0.0",
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf(
                    "labels" to schemaMapOf("identifier" to "Person")
                )
            ),
            "extensions" to schemaMapOf(
                "data_source_mappings" to schemaMapOf(
                    "mapping0" to listOf(
                        schemaMapOf(
                            "table" to "raw_events",
                            "query" to "UNWIND \$rows AS row CREATE (n:Person {id: row.id})"
                        )
                    )
                )
            )
        )

        val gm3 = gsToGm3.migrate(gsInput)
        val expectedQueryMapping = schemaMapOf(
            "table" to "raw_events",
            "query" to "UNWIND \$rows AS row CREATE (n:Person {id: row.id})"
        )

        val queryMappings = assertNotNull(
            gm3.mapOrNull("graphSchemaExtensionsRepresentation")?.listOfMapsOrNull("queryMappings"),
            "gsToGm3 must carry QueryMapping entries into graphSchemaExtensionsRepresentation.queryMappings - currently errors at GraphSpecDataModelV3Migration.kt:87 (convertGraphMapping) for any entry whose only body is `query`"
        )

        assertEquals(
            listOf(expectedQueryMapping),
            queryMappings,
            "QueryMapping entry not preserved on gs-gm3 migration: input had one query mapping for table raw_events, gm3 output should contain the same entry"
        )
    }

    @Test
    fun `gm3 to gs carries queryMappings extension entries into extensions data_source_mappings mapping0`() {
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
            "graphSchemaExtensionsRepresentation" to schemaMapOf(
                "queryMappings" to listOf(
                    schemaMapOf("table" to "raw_events", "query" to "UNWIND \$rows AS row CREATE (n:Person {id: row.id})")
                )
            ),
            "graphMappingRepresentation" to schemaMapOf(
                "dataSourceSchema" to schemaMapOf("type" to "local", "tableSchemas" to emptyList<Any>()),
                "nodeMappings" to emptyList<Any>(),
                "relationshipMappings" to emptyList<Any>()
            ),
            "configurations" to schemaMapOf("idsToIgnore" to emptyList<String>())
        )

        val gs = gm3ToGs.migrate(gm3Input)
        val expectedEntry = schemaMapOf(
            "table" to "raw_events",
            "query" to "UNWIND \$rows AS row CREATE (n:Person {id: row.id})"
        )

        val mappingEntries = assertNotNull(
            gs.mapOrNull("extensions")
                ?.mapOrNull("data_source_mappings")
                ?.listOfMapsOrNull("mapping0"),
            "extensions.data_source_mappings.mapping0 should be present after gm3 to gs migration"
        )

        assertEquals(
            listOf(expectedEntry),
            mappingEntries,
            "gm3 to gs drops queryMappings extension section entirely - input had one query mapping under graphSchemaExtensionsRepresentation.queryMappings, expected entry under extensions.data_source_mappings.mapping0; the migration neither reads graphSchemaExtensionsRepresentation.queryMappings nor emits equivalent entries"
        )
    }
}
