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

class MappingModeNotTransferredGsToGm3RoundTripTest {

    private val gsToGm3 = GraphSpecDataModelV3Migration()
    private val gm3ToGs = DataModelV3GraphSpecMigration()

    @Test
    fun `gs to gm3 to gs round-trip preserves CREATE MappingMode on node mapping`() {
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
                            "node" to "n1",
                            "table" to "users",
                            "mode" to "CREATE",
                            "properties" to schemaMapOf(
                                "p1" to schemaMapOf("column" to "id")
                            )
                        )
                    )
                )
            )
        )

        val gsRoundTripped = gsInput.let(gsToGm3::migrate).let(gm3ToGs::migrate)
        val nodeMapping = assertNotNull(
            gsRoundTripped
                .map("extensions")
                .map("data_source_mappings")
                .listOfMapsOrNull("mapping0")
                ?.firstOrNull { it.stringOrNull("node") == "n1" },
            "node mapping listing absent from extensions.data_source_mappings.mapping0 after gs-gm3-gs round-trip",
        )

        assertEquals(
            "CREATE",
            nodeMapping.stringOrNull("mode"),
            "MappingMode slot dropped on gs-gm3-gs round-trip: not emitted by gsToGm3 at GraphSpecDataModelV3Migration.kt:71-75 (convertGraphMapping node assembly) and not restored by gm3ToGs at DataModelV3GraphSpecMigration.kt:311-317 (nodeMappings mapping assembly)"
        )
    }
}
