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
        val schema = File(SCHEMA).readText()
        val id = Regex(ID_PATTERN).find(schema)?.groupValues?.get(1)

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

    @Test
    fun `a released spec version has an unchanged schema`() {
        val tag = TAG_PREFIX + schemaVersion()
        val released = git("show", "$tag:$SCHEMA")
        assumeTrue(released != null, "Spec ${schemaVersion()} is not released")

        assertEquals(
            withoutId(released!!),
            withoutId(File(SCHEMA).readText()),
            "Spec ${schemaVersion()} is already released ($tag) but $SCHEMA has changed since. Bump the spec " +
                "version (\$id in $SCHEMA and Version.LATEST) so the published schema is not overwritten."
        )
    }

    @Test
    fun `an unreleased spec version is higher than every released version`() {
        val version = schemaVersion()
        val tags = releaseTags()
        assumeTrue(tags.isNotEmpty() && TAG_PREFIX + version !in tags, "Spec $version is released or nothing is")

        val highest = tags.map { it.removePrefix(TAG_PREFIX) }.maxWith(::compareVersions)
        assertTrue(
            compareVersions(version, highest) > 0,
            "Spec version $version must be higher than the latest released spec version $highest."
        )
    }

    private fun schemaVersion(): String =
        Version.parseSchemaVersion(Regex(ID_PATTERN).find(File(SCHEMA).readText())!!.groupValues[1])

    private fun withoutId(schema: String) = schema.lines().filterNot { it.contains("\"\$id\"") }

    private fun compareVersions(a: String, b: String): Int {
        val left = a.split('.').map { it.toInt() }
        val right = b.split('.').map { it.toInt() }
        return left.zip(right).map { (x, y) -> x.compareTo(y) }.firstOrNull { it != 0 } ?: 0
    }

    private fun releaseTags(): List<String> = git("tag", "-l", "$TAG_PREFIX*")?.lines().orEmpty().filter { it.isNotBlank() }

    /**
     * Output of the git command, or null when it fails or git is not installed.
     */
    private fun git(vararg args: String): String? = try {
        val process = ProcessBuilder("git", *args).redirectError(ProcessBuilder.Redirect.DISCARD).start()
        val output = process.inputStream.bufferedReader().readText()
        if (process.waitFor() == 0) output else null
    } catch (_: IOException) {
        null
    }

    private companion object {
        const val TAG_PREFIX = "release-spec/v"
        const val SCHEMA = "ontology-graph-spec.schema.json"
        const val ID_PATTERN = "\"\\\$id\"\\s*:\\s*\"([^\"]+)\""
    }
}
