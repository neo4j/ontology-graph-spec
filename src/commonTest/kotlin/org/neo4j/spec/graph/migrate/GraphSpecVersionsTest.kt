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
package org.neo4j.spec.graph.migrate

import org.neo4j.spec.graph.GraphSpec
import org.neo4j.spec.graph.codec.schema.schemaMapOf
import org.neo4j.spec.graph.migrate.migration.GraphSpecNoOpMigration
import org.neo4j.spec.graph.model.Type
import org.neo4j.spec.graph.model.Version
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GraphSpecVersionsTest {

    @Test
    fun `versions are ordered, distinct and end with latest`() {
        assertEquals(Version.LATEST, Version.GRAPH_SPEC_VERSIONS.last())
        assertEquals(Version.GRAPH_SPEC_VERSIONS.distinct(), Version.GRAPH_SPEC_VERSIONS)
    }

    @Test
    fun `every released spec version migrates to latest`() {
        val path = MigrationPath(GraphSpec.Json.configuration.migrations)
        for (version in Version.GRAPH_SPEC_VERSIONS) {
            assertNotNull(
                path.findPath("${Type.GRAPH_SPEC}:$version", "${Type.GRAPH_SPEC}:${Version.LATEST}"),
                "No migration from graph spec $version to ${Version.LATEST}"
            )
        }
    }

    @Test
    fun `latest spec migrates to every data model export`() {
        val path = MigrationPath(GraphSpec.Json.configuration.migrations)
        assertNotNull(path.findPath("${Type.GRAPH_SPEC}:${Version.LATEST}", "${Type.DATA_MODEL}:${Version.DATA_MODEL_V3_0}"))
    }

    @Test
    fun `no-op migration only bumps the schema version`() {
        val path = MigrationPath(mapOf("graph_spec:1.0.0" to listOf(GraphSpecNoOpMigration("1.0.0", "1.1.0"))))
        val schema = schemaMapOf("schema" to Version.schemaUrl("1.0.0"), "version" to "7", "id" to "x")

        val migrated = path.migrate(schema, Type.GRAPH_SPEC, "1.1.0", Type.GRAPH_SPEC)

        assertEquals(Version.schemaUrl("1.1.0"), migrated.string("schema"))
        assertEquals("7", migrated.string("version"))
        assertEquals("x", migrated.string("id"))
    }
}
