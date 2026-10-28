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
package org.neo4j.spec.graph.model

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Keeps the published schema and the git release tags in step with [Version]. Run from the project directory.
 */
class SpecVersionConsistencyTest {

    @Test
    fun `schema id is the latest spec version`() {
        val schema = File("ontology-graph-spec.schema.json").readText()
        val id = Regex("\"\\\$id\"\\s*:\\s*\"([^\"]+)\"").find(schema)?.groupValues?.get(1)

        assertEquals(Version.schemaUrl(Version.LATEST), id)
    }

    @Test
    fun `every released spec version is listed`() {
        val tags = releaseTags()
        assumeTrue(tags.isNotEmpty(), "No release-spec tags available")

        for (tag in tags) {
            val version = tag.removePrefix(TAG_PREFIX)
            assertTrue(
                version in Version.GRAPH_SPEC_VERSIONS,
                "Released spec $version is missing from Version.GRAPH_SPEC_VERSIONS. Add it, with a migration " +
                    "(GraphSpecNoOpMigration if nothing changed) to ${Version.LATEST}."
            )
        }
    }

    private fun releaseTags(): List<String> = try {
        val process = ProcessBuilder("git", "tag", "-l", "$TAG_PREFIX*").redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        if (process.waitFor() == 0) output.lines().filter { it.isNotBlank() } else emptyList()
    } catch (_: IOException) {
        emptyList() // git is not installed
    }

    private companion object {
        const val TAG_PREFIX = "release-spec/v"
    }
}
