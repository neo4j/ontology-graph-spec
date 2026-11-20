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
package org.neo4j.spec.graph.validate

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.Version
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchemaUrlTest {

    @Test
    fun `released schema urls are valid`() {
        for (version in Version.GRAPH_SPEC_VERSIONS) {
            val issues = mutableListOf<Issue>()
            SchemaUrl.validate(GraphModel(version = "1", id = "test", schema = Version.schemaUrl(version)), issues)
            assertTrue(issues.isEmpty(), "Expected no issues for $version")
        }
    }

    @Test
    fun `whitespace trailing slashes and missing file name are tolerated`() {
        val url = Version.schemaUrl(Version.V1)
        for (schema in listOf("$url/", " $url ", url.removeSuffix("/schema.json"), url.removeSuffix("schema.json"))) {
            val issues = mutableListOf<Issue>()
            SchemaUrl.validate(GraphModel(version = "1", id = "test", schema = schema), issues)
            assertTrue(issues.isEmpty(), "Expected no issues for '$schema'")
        }
    }

    @Test
    fun `unknown version is reported`() {
        val issues = mutableListOf<Issue>()
        SchemaUrl.validate(GraphModel(version = "1", id = "test", schema = Version.schemaUrl("9.9.9")), issues)

        assertEquals(1, issues.size)
    }

    @Test
    fun `unknown schema url is reported`() {
        val issues = mutableListOf<Issue>()
        SchemaUrl.validate(GraphModel(version = "1", id = "test", schema = "https://example.com/schema.json"), issues)

        assertEquals(1, issues.size)
        assertEquals("invalid_schema_url", issues.first().code)
        assertEquals("/schema", issues.first().path)
    }
}
