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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.neo4j.spec.graph.GraphSpec
import org.neo4j.spec.graph.model.Type
import org.neo4j.spec.graph.model.Version
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CodecTest {
    private val input = """{
        "schema":"https://neo4j.com/docs/ontology-graph-spec/1.1.0/schema.json",
        "version":"1","id":"codec","nodes":{"person":{"label":"Person"}}
    }
    """.trimIndent()

    @Test
    fun decodeCarriesDefaultsAndUsesThePublicCodecForBothFormats() {
        val reference = GraphSpec.Json.decodeFromString(input)
        val transport = decodeContent(input, "json", Type.GRAPH_SPEC)
        assertContains(transport, "\"description\":\"\"")
        assertEquals(
            GraphSpec.Json.encodeToString(reference),
            encodeContent(transport, "json", Type.GRAPH_SPEC, Version.LATEST)
        )
        val yaml = encodeContent(transport, "yaml", Type.GRAPH_SPEC, Version.LATEST)
        assertEquals(GraphSpec.Yaml.encodeToString(reference), yaml)
        assertEquals(transport, decodeContent(yaml, "yaml", Type.GRAPH_SPEC))
    }

    @Test
    fun directEncodingPreservesShorthand() {
        val encoded = encodeContent(input, "json", Type.GRAPH_SPEC, Version.LATEST)
        val node = Json.parseToJsonElement(encoded).jsonObject["nodes"]!!.jsonObject["person"]!!.jsonObject
        assertEquals("Person", node["label"]!!.toString().trim('"'))
        assertEquals(null, node["labels"])
    }

    @Test
    fun yamlMigrationUsesTheSameRoutes() {
        val transport = decodeContent(input, "json", Type.GRAPH_SPEC)
        val exported = encodeContent(transport, "yaml", Type.DATA_MODEL, Version.DATA_MODEL_V3_0)
        val migrated = decodeContent(exported, "yaml", Type.DATA_MODEL)
        assertContains(migrated, "Person")
        assertContains(migrated, Version.LATEST)
    }

    @Test
    fun unsupportedFormatsFailWithADiagnostic() {
        val error = assertFailsWith<IllegalArgumentException> { decodeContent(input, "xml", Type.GRAPH_SPEC) }
        assertContains(error.message!!, "unknown document format: xml")
        assertFailsWith<IllegalArgumentException> { encodeContent(input, "xml", Type.GRAPH_SPEC, Version.LATEST) }
    }
}
