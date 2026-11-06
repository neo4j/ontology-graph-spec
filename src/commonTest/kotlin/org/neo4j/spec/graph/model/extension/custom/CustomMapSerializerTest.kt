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
package org.neo4j.spec.graph.model.extension.custom

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.neo4j.spec.graph.codec.format.JsonFormat
import org.neo4j.spec.graph.codec.format.YamlFormat
import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.extension.GraphModelExtensions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CustomMapSerializerTest {

    private val custom: MutableMap<String, Any> = mutableMapOf(
        "string" to "text",
        "numeric_string" to "5",
        "negative_string" to "-3",
        "boolean_string" to "true",
        "boolean" to true,
        "long" to 5L,
        "negative" to -3L,
        "double" to 1.5,
        "negative_double" to -2.25,
        "list" to mutableListOf(1L, "x", false),
        "map" to mutableMapOf("nested" to mutableMapOf<String, Any>("deep" to 10L))
    )

    private fun model(custom: MutableMap<String, Any>) = GraphModel(
        version = "1",
        id = "test",
        extensions = GraphModelExtensions(custom = custom)
    )

    @Test
    fun `json writes plain values`() {
        val encoded = JsonFormat.default.encodeModelToString(model(mutableMapOf("a" to "b", "c" to 1L)))
        assertTrue(encoded.contains("\"a\": \"b\""), encoded)
        assertTrue(encoded.contains("\"c\": 1"), encoded)
    }

    @Test
    fun `json round trip keeps implicit types`() {
        val format = JsonFormat.default
        val decoded = format.decodeModelFromString(format.encodeModelToString(model(custom)))
        assertTyped(custom, decoded.extensions.custom)
    }

    @Test
    fun `yaml round trip keeps implicit types`() {
        val format = YamlFormat.default
        val decoded = format.decodeModelFromString(format.encodeModelToString(model(custom)))
        assertTyped(custom, decoded.extensions.custom)
    }

    @Test
    fun `yaml distinguishes quoted and unquoted scalars`() {
        val yaml = """
            schema: "https://neo4j.com/docs/ontology-graph-spec/1.0.0/schema.json"
            version: "1"
            id: "test"
            extensions:
              custom:
                quoted: "5"
                unquoted: 5
                negative: -3
                decimal: 1.5
                flag: true
                list: [1, "x"]
                map: { f: true }
        """.trimIndent()
        val custom = YamlFormat.default.decodeModelFromString(yaml).extensions.custom
        assertTyped(
            mapOf(
                "quoted" to "5",
                "unquoted" to 5L,
                "negative" to -3L,
                "decimal" to 1.5,
                "flag" to true,
                "list" to listOf(1L, "x"),
                "map" to mapOf("f" to true)
            ),
            custom
        )
    }

    @Test
    fun `int and float values are widened`() {
        val format = JsonFormat.default
        val decoded = format.decodeModelFromString(
            format.encodeModelToString(model(mutableMapOf("i" to 1, "f" to 0.5f)))
        )
        assertTyped(mapOf("i" to 1L, "f" to 0.5), decoded.extensions.custom)
    }

    @Test
    fun `unsupported value type fails to encode`() {
        assertFailsWith<SerializationException> {
            JsonFormat.default.encodeModelToString(model(mutableMapOf("bad" to Pair(1, 2))))
        }
    }

    @Test
    fun `raw sets encode as lists and decode as lists`() {
        val format = JsonFormat.default
        val encoded = format.encodeModelToString(
            model(mutableMapOf("tags" to setOf("a", "b"), "n" to setOf(setOf(1L))))
        )
        val decoded = format.decodeModelFromString(encoded).extensions.custom

        assertEquals(listOf("a", "b"), decoded["tags"])
        assertEquals(listOf(listOf(1L)), decoded["n"])
    }

    @Test
    fun `deeply nested sets fail to encode`() {
        var value: Any = "x"
        repeat(MAX_CUSTOM_DEPTH + 1) { value = setOf(value) }
        assertFailsWith<SerializationException> {
            JsonFormat.default.encodeModelToString(model(mutableMapOf("deep" to value)))
        }
    }

    @Test
    fun `unsupported value type error points to setSerializable`() {
        val values = listOf(Pair(1, 2), JsonObject(mapOf("a" to JsonPrimitive(1))), JsonArray(listOf(JsonPrimitive(1))))
        for (value in values) {
            val error = assertFailsWith<SerializationException> {
                JsonFormat.default.encodeModelToString(model(mutableMapOf("bad" to value)))
            }
            assertTrue(error.message!!.contains("setSerializable"), error.message)
        }
    }

    @Test
    fun `null value fails to decode`() {
        val json = """
            {"schema": "https://neo4j.com/docs/ontology-graph-spec/1.0.0/schema.json", "version": "1", "id": "test",
             "extensions": {"custom": {"a": null}}}
        """.trimIndent()
        assertFailsWith<SerializationException> {
            JsonFormat.default.decodeModelFromString(json)
        }
    }

    @Test
    fun `deeply nested values fail to encode`() {
        var nested: Any = "leaf"
        repeat(MAX_CUSTOM_DEPTH + 1) { nested = mutableListOf(nested) }
        assertFailsWith<SerializationException> {
            JsonFormat.default.encodeModelToString(model(mutableMapOf("deep" to nested)))
        }
    }

    @Test
    fun `cyclic values fail to encode`() {
        val cycle = mutableMapOf<String, Any>()
        cycle["self"] = cycle
        assertFailsWith<SerializationException> {
            JsonFormat.default.encodeModelToString(model(mutableMapOf("cycle" to cycle)))
        }
    }

    @Test
    fun `deeply nested values fail to decode`() {
        val depth = MAX_CUSTOM_DEPTH * 4
        val json = """
            {"schema": "https://neo4j.com/docs/ontology-graph-spec/1.0.0/schema.json", "version": "1", "id": "test",
             "extensions": {"custom": {"a": ${"[".repeat(depth)}${"]".repeat(depth)}}}}
        """.trimIndent()
        assertFailsWith<SerializationException> {
            JsonFormat.default.decodeModelFromString(json)
        }
    }

    @Test
    fun `values at the depth limit round trip`() {
        var nested: Any = "leaf"
        repeat(MAX_CUSTOM_DEPTH) { nested = mutableListOf(nested) }
        val format = JsonFormat.default
        val decoded = format.decodeModelFromString(format.encodeModelToString(model(mutableMapOf("deep" to nested))))
        assertEquals(nested, decoded.extensions.custom["deep"])
    }

    private fun assertTyped(expected: Map<String, Any>, actual: Map<String, Any>) {
        assertEquals(typed(expected), typed(actual))
        assertEquals(expected, actual)
    }

    private fun typed(value: Any?): Any? = when (value) {
        is Map<*, *> -> value.mapValues { typed(it.value) }
        is List<*> -> value.map { typed(it) }
        null -> null
        else -> "${value::class.simpleName}($value)"
    }
}
