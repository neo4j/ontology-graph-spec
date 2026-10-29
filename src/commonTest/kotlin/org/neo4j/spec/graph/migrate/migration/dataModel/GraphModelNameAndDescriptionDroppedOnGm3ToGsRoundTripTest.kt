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
import org.neo4j.spec.graph.model.Version
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphModelNameAndDescriptionDroppedOnGm3ToGsRoundTripTest {

    private val gsToGm3 = GraphSpecDataModelV3Migration()
    private val gm3ToGs = DataModelV3GraphSpecMigration()
    private val gsToWrappedGm3 = GraphSpecDataModelWrappedMigration()
    private val unwrap = DataModelUnwrapMigration(Version.DATA_MODEL_V30)

    @Test
    fun `gs to gm3 to gs round-trip does not preserve top-level name`() {
        val gsInput = schemaMapOf(
            "version" to "1.0.0",
            "name" to "Import1",
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf("labels" to schemaMapOf("identifier" to "Person"))
            )
        )

        val gsRoundTripped = gsInput.let(gsToGm3::migrate).let(gm3ToGs::migrate)
        assertEquals(
            "Import1",
            gsRoundTripped.stringOrNull("name"),
            "top-level 'name' should round-trip gs to gm3 to gs; no converter emits 'name' on the gm3 side (no representation, no impl) - description uses wrapped envelope and survives, name does not. Find gap location: emitName equivalent absent at DataModelV3GraphSpecMigration.kt:43-69 (graphModel envelope assembly)",
        )
    }

    @Test
    fun `description round-trips via wrapped gm3 envelope then unwrap then flat gm3 then gs`() {
        val gsInput = schemaMapOf(
            "version" to "1.0.0",
            "description" to "Customer data",
            "nodes" to schemaMapOf(
                "n1" to schemaMapOf("labels" to schemaMapOf("identifier" to "Person"))
            )
        )

        val gsRoundTripped = gsInput
            .let(gsToWrappedGm3::migrate)
            .let(unwrap::migrate)
            .let(gm3ToGs::migrate)
        assertEquals(
            "Customer data",
            gsRoundTripped.stringOrNull("description"),
            "description should round-trip via wrapped envelope (GraphSpecDataModelWrappedMigration.kt:46 emits it; DataModelWrapper moves it across wrap/unwrap)"
        )
    }
}
