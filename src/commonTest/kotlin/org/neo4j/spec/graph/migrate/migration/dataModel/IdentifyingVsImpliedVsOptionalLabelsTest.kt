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

import org.neo4j.spec.graph.codec.schema.SchemaList
import org.neo4j.spec.graph.codec.schema.SchemaLiteral
import org.neo4j.spec.graph.codec.schema.schemaMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IdentifyingVsImpliedVsOptionalLabelsTest {

    private val gsToGm3 = GraphSpecDataModelV3Migration()
    private val gm3ToGs = DataModelV3GraphSpecMigration()

    @Test
    fun `gs to gm3 to gs round-trip preserves identifier implied and optional slots`() {
        val gsInput = schemaMapOf(
            "version" to "1.0.0",
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf(
                    "labels" to schemaMapOf(
                        "identifier" to "Person",
                        "implied" to listOf("Premium"),
                        "optional" to listOf("Eligible")
                    )
                )
            )
        )

        val gsRoundTripped = gsToGm3.migrate(gsInput).let(gm3ToGs::migrate)
        val labels = gsRoundTripped.mapOfMaps("nodes")["n1"]!!.map("labels")

        assertEquals(
            "Person",
            assertNotNull(
                labels["identifier"] as? SchemaLiteral,
                "identifier slot dropped on gs-gm3-gs round-trip",
            ).string,
        )
        assertEquals(
            listOf("Premium"),
            assertNotNull(
                labels["implied"] as? SchemaList,
                "implied slot dropped on gs-gm3-gs round-trip",
            ).map { (it as SchemaLiteral).string },
            "implied contents overlap the optional slot through DataModelV3GraphSpecMigration.kt:151",
        )
        assertEquals(
            listOf("Eligible"),
            assertNotNull(
                labels["optional"] as? SchemaList,
                "optional slot should round-trip via gs to gm3 to gs - currently dropped at DataModelV3GraphSpecMigration.kt:151",
            ).map { (it as SchemaLiteral).string },
        )
    }
}
