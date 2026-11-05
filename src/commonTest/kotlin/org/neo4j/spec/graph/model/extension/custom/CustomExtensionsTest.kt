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

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.neo4j.spec.graph.codec.format.JsonFormat
import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.extension.GraphModelExtensions
import org.neo4j.spec.graph.model.extension.getSerializable
import org.neo4j.spec.graph.model.extension.setSerializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@Serializable
private data class Point(val x: Int, val y: Double, val label: String = "origin", val tags: List<String> = emptyList())

@Serializable
private data class Tagged(val tags: Set<String>, val nested: Set<Point> = emptySet())

@Serializable
private data class Chain(val next: Chain? = null)

@Serializable
private data class Box<T>(val value: T, val nested: Point? = null)

class CustomExtensionsTest {

    private val extensions = GraphModelExtensions()

    @Test
    fun `serializable objects can be read back`() {
        extensions.setSerializable("point", Point(1, 2.5, tags = listOf("a")))
        extensions.setSerializable("points", listOf(Point(1, 1.0), Point(2, 2.0)))

        assertEquals(Point(1, 2.5, tags = listOf("a")), extensions.getSerializable<Point>("point"))
        assertEquals(listOf(Point(1, 1.0), Point(2, 2.0)), extensions.getSerializable<List<Point>>("points"))
        assertEquals(Point(1, 2.5, tags = listOf("a")), extensions.getSerializable("point", Point.serializer()))
        assertNull(extensions.getSerializable<Point>("missing"))
    }

    @Test
    fun `serializable objects survive a round trip through a file`() {
        extensions.setSerializable("section", "point", Point(1, 2.5))
        val format = JsonFormat.default
        val model = GraphModel(
            version = "1",
            id = "test",
            extensions = extensions
        )
        val decoded = format.decodeModelFromString(format.encodeModelToString(model)).extensions

        assertEquals(Point(1, 2.5), decoded.getSerializable<Point>("section", "point"))
    }

    @Test
    fun `reading a mismatched value fails`() {
        extensions.custom["point"] = "text"

        assertFailsWith<SerializationException> { extensions.getSerializable<Point>("point") }
        assertNull(extensions.getSerializable<Point>("point", "x"))
    }

    @Test
    fun `serializable objects are stored as plain values`() {
        extensions.setSerializable("point", Point(1, 2.5, tags = listOf("a")))

        assertEquals(
            mapOf("x" to 1L, "y" to 2.5, "label" to "origin", "tags" to listOf("a")),
            extensions.custom["point"]
        )
    }

    @Test
    fun `serializable objects can be stored in a section`() {
        extensions.custom["section"] = mapOf("other" to "x")
        extensions.setSerializable("section", "point", Point(1, 2.0))

        assertEquals(Point(1, 2.0), extensions.getSerializable<Point>("section", "point"))
        assertEquals("x", (extensions.custom["section"] as Map<*, *>)["other"])
    }

    @Test
    fun `generic classes and collections are supported`() {
        extensions.setSerializable("box", Box(listOf(1, 2), Point(0, 0.0)))
        extensions.setSerializable("points", listOf(Point(1, 1.0)))
        extensions.setSerializable("number", 5)
        extensions.setSerializable("explicit", Box("a"), Box.serializer(String.serializer()))

        val box = extensions.custom["box"] as Map<*, *>
        assertEquals(listOf(1L, 2L), box["value"])
        assertEquals("origin", (box["nested"] as Map<*, *>)["label"])
        assertEquals(1, (extensions.custom["points"] as List<*>).size)
        assertEquals(5L, extensions.custom["number"])
        assertEquals("a", (extensions.custom["explicit"] as Map<*, *>)["value"])
    }

    @Test
    fun `sets are stored as lists and read back as sets`() {
        extensions.setSerializable("tags", setOf("b", "a"))
        extensions.setSerializable("tagged", Tagged(setOf("x", "y"), setOf(Point(1, 1.0))))

        assertEquals(listOf("b", "a"), extensions.custom["tags"])
        assertEquals(setOf("b", "a"), extensions.getSerializable<Set<String>>("tags"))
        assertEquals(Tagged(setOf("x", "y"), setOf(Point(1, 1.0))), extensions.getSerializable<Tagged>("tagged"))
        assertEquals(listOf("x", "y"), (extensions.custom["tagged"] as Map<*, *>)["tags"])
    }

    @Test
    fun `sets survive a round trip through a file`() {
        extensions.setSerializable("section", "tagged", Tagged(setOf("x", "y")))
        val format = JsonFormat.default
        val model = GraphModel(version = "1", id = "test", extensions = extensions)
        val decoded = format.decodeModelFromString(format.encodeModelToString(model)).extensions

        assertEquals(Tagged(setOf("x", "y")), decoded.getSerializable<Tagged>("section", "tagged"))
    }

    @Test
    fun `json elements are converted to plain values`() {
        extensions.setSerializable<JsonElement>(
            "json",
            JsonObject(
                mapOf(
                    "a" to JsonPrimitive(1),
                    "b" to JsonPrimitive("x")
                )
            )
        )

        assertEquals(mapOf("a" to 1L, "b" to "x"), extensions.custom["json"])
    }

    @Test
    fun `serializable objects round trip as plain maps`() {
        extensions.setSerializable("point", Point(1, 2.5))
        val format = JsonFormat.default
        val model = GraphModel(
            version = "1",
            id = "test",
            extensions = extensions
        )
        val decoded = format.decodeModelFromString(format.encodeModelToString(model)).extensions

        assertEquals(extensions.custom["point"], decoded.custom["point"])
    }

    @Test
    fun `null and too deeply nested serializable values are rejected`() {
        assertFailsWith<SerializationException> { extensions.setSerializable<Point?>("null", null) }
        assertFailsWith<SerializationException> { extensions.setSerializable("deep", chain(MAX_CUSTOM_DEPTH + 1)) }
        extensions.setSerializable("ok", chain(MAX_CUSTOM_DEPTH - 1))
    }

    private fun chain(depth: Int): Chain = (1 until depth).fold(Chain()) { acc, _ -> Chain(acc) }
}
