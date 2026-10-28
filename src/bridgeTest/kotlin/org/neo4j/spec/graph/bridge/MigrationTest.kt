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
package org.neo4j.spec.graph.bridge

import org.neo4j.spec.graph.GraphSpec
import org.neo4j.spec.graph.codec.schema.SchemaMap
import org.neo4j.spec.graph.model.Type
import org.neo4j.spec.graph.model.Version
import kotlin.test.Test
import kotlin.test.assertEquals

class MigrationTest {

    @Test
    fun testMigrate() {
        val shorthand = """{
            "schema": "${Version.schemaUrl(Version.LATEST)}",
            "version": "1",
            "id": "test",
            "nodes": {
                "person": {
                    "label": "Person",
                    "properties": { "id": { "type": "STRING", "unique": true } }
                }
            }
        }
        """.trimIndent()

        val result = migrateContent(shorthand, Type.GRAPH_SPEC, Type.DATA_MODEL, Version.DATA_MODEL_V3_0)

        val dataModel = GraphSpec.Json.configuration.format.decodeFromString(result) as SchemaMap
        val constraints = dataModel.map("graphSchemaRepresentation").map("graphSchema").list("constraints")
        assertEquals(1, constraints.size)
    }
}
