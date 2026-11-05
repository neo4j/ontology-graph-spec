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
package org.neo4j.spec.graph.codec.format

import kotlinx.serialization.SerializationException
import org.neo4j.spec.graph.codec.schema.SchemaMap
import org.neo4j.spec.graph.codec.schema.SchemaNull
import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.extension.GraphModelExtensions
import org.neo4j.spec.graph.model.extension.mapping.NodeMapping
import org.neo4j.spec.graph.model.extension.mapping.PropertyMapping
import org.neo4j.spec.graph.model.extension.mapping.QueryMapping
import org.neo4j.spec.graph.model.extension.mapping.RelationshipMapping
import org.neo4j.spec.graph.model.extension.mapping.TargetMapping
import org.neo4j.spec.graph.model.index.FullTextIndexOption
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.node.extension.NodeExtensions
import org.neo4j.spec.graph.model.node.extension.NodeIndex
import org.neo4j.spec.graph.model.type.IndexType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsonFormatTest {

    private val jsonFormat: Format = JsonFormat.default

    @Test
    fun `test nested path generation for maps and lists`() {
        val json = """
        {
            "nodes": {
                "user": {
                    "properties": {
                        "age": "30"
                    }
                }
            },
            "mappings": [
                { "id": "m1" }
            ]
        }
        """.trimIndent()

        val schema = jsonFormat.decodeFromString(json) as SchemaMap

        // Verify Map Pathing
        assertEquals(
            "nodes.user.properties.age",
            schema.map("nodes").map("user").map("properties").literal("age").path
        )

        // Verify List Pathing
        val mappings = schema.list("mappings")
        assertEquals("mappings", mappings.path)
        val firstMapping = mappings[0] as SchemaMap
        assertEquals("mappings[0]", firstMapping.path)
        assertEquals("mappings[0].id", firstMapping.literal("id").path)
    }

    @Test
    fun `test SchemaNull edge case in maps`() {
        val json = """{ "missing": null, "present": "value" }"""
        val schema = jsonFormat.decodeFromString(json) as SchemaMap

        assertIs<SchemaNull>(schema.content["missing"])
        assertEquals("missing", schema.content["missing"]?.path)

        val backToJson = jsonFormat.encodeToString(schema)
        assertTrue(backToJson.contains("\"missing\": null"))
    }

    @Test
    fun `test list and map extraction helpers`() {
        val json = """
        {
            "list_of_maps": [{ "id": 1 }, { "id": 2 }],
            "map_of_maps": { 
                "a": { "v": "hi" },
                "b": { "v": "bye" }
            }
        }
        """.trimIndent()
        val schema = jsonFormat.decodeFromString(json) as SchemaMap

        val listOfMaps = schema.listOfMaps("list_of_maps")
        assertEquals(2, listOfMaps.size)
        assertEquals("1", listOfMaps[0].string("id"))

        val mapOfMaps = schema.mapOfMaps("map_of_maps")
        assertEquals("hi", mapOfMaps["a"]?.string("v"))
    }

    @Test
    fun `test empty collections round-trip`() {
        val model = GraphModel(
            version = "1.0",
            id = "test",
            nodes = mutableMapOf(),
            extensions = GraphModelExtensions(dataSourceMappings = mutableMapOf())
        )

        val schema = jsonFormat.encodeToSchema(model)
        val decoded = jsonFormat.decodeFromSchema(schema)

        assertEquals(model.version, decoded.version)
        assertTrue(decoded.nodes.isEmpty())
        assertTrue(decoded.extensions.dataSourceMappings.isEmpty())
    }

    @Test
    fun `test custom extension values serialize with implicit types`() {
        val node = Node(
            extensions = NodeExtensions(
                custom = mutableMapOf(
                    "ext_str" to "hello",
                    "ext_bool" to true
                )
            )
        )
        val model =
            GraphModel(
                version = "1",
                id = "test",
                nodes = mutableMapOf("n1" to node)
            )

        val encoded = jsonFormat.encodeModelToString(model)
        val schema = jsonFormat.decodeFromString(encoded) as SchemaMap
        val n1 = schema.map("nodes").map("n1")
        val extensions = n1.map("extensions").map("custom")

        assertEquals("hello", extensions.string("ext_str"))
        assertTrue(extensions.literal("ext_str").isString)
        assertEquals("true", extensions.string("ext_bool"))
        assertFalse(extensions.literal("ext_bool").isString)

        val decoded = jsonFormat.decodeModelFromString(encoded)
        val decodedNode = decoded.nodes["n1"]!!
        assertEquals("hello", decodedNode.extensions.custom["ext_str"])
        assertEquals(true, decodedNode.extensions.custom["ext_bool"])
    }

    @Test
    fun `test polymorphic Mapping serialization round-trips through explicit type discriminator`() {
        val model = GraphModel(
            version = "1",
            id = "test",
            extensions = GraphModelExtensions(
                dataSourceMappings = mutableMapOf(
                    "m0" to mutableListOf(
                        NodeMapping(
                            node = "n0",
                            table = "t0",
                            properties = mutableMapOf("p0" to PropertyMapping(column = "f0"))
                        ),
                        RelationshipMapping(
                            relationship = "r0",
                            table = "t1",
                            fromNode = TargetMapping(),
                            toNode = TargetMapping()
                        ),
                        QueryMapping(table = "t2", query = "MATCH (n) RETURN n")
                    )
                )
            )
        )

        val encoded = jsonFormat.encodeModelToString(model)
        assertTrue(encoded.contains(""""type": "NODE""""))
        assertTrue(encoded.contains(""""type": "RELATIONSHIP""""))
        assertTrue(encoded.contains(""""type": "QUERY""""))

        val decoded = jsonFormat.decodeModelFromString(encoded)
        assertEquals(model.extensions.dataSourceMappings, decoded.extensions.dataSourceMappings)
    }

    @Test
    fun `test IndexOption with every field left at its default still round-trips`() {
        val model = GraphModel(
            version = "1",
            id = "test",
            nodes = mutableMapOf(
                "n0" to Node(
                    extensions = NodeExtensions(
                        indexes = mutableMapOf(
                            "idx0" to NodeIndex(
                                type = IndexType.FULLTEXT,
                                labels = mutableListOf("Document"),
                                properties = mutableListOf("body"),
                                options = FullTextIndexOption()
                            )
                        )
                    )
                )
            )
        )

        val encoded = jsonFormat.encodeModelToString(model)
        val decoded = jsonFormat.decodeModelFromString(encoded)

        assertEquals(FullTextIndexOption(), decoded.nodes["n0"]!!.extensions.indexes["idx0"]!!.options)
    }

    @Test
    fun `test decoding a Mapping without a type discriminator fails clearly`() {
        val json = """{ "node": "n0", "table": "t0", "properties": {} }"""

        val failure = assertFailsWith<SerializationException> {
            jsonFormat.decodeModelFromString("""{"version":"1","extensions":{"data_source_mappings":{"m0":[$json]}}}""")
        }

        assertTrue(
            failure.message!!.contains("discriminator", ignoreCase = true),
            "unexpected message: ${failure.message}"
        )
    }

    @Test
    fun `test decoding a Mapping with an unknown type discriminator fails clearly`() {
        val json = """{ "type": "NotARealMapping", "node": "n0", "table": "t0", "properties": {} }"""

        assertFailsWith<SerializationException> {
            jsonFormat.decodeModelFromString("""{"version":"1","extensions":{"data_source_mappings":{"m0":[$json]}}}""")
        }
    }

    @Test
    fun `test error handling for missing keys or wrong types`() {
        val json = """{ "id": "123", "meta": { "active": true } }"""
        val schema = jsonFormat.decodeFromString(json) as SchemaMap

        // Try to access a literal as a map
        val ex1 = assertFailsWith<IllegalStateException> { schema.map("id") }
        assertTrue(ex1.message!!.contains("Expected map, found invalid type SchemaLiteral at id"))

        // Try to access a missing key
        val ex2 = assertFailsWith<IllegalStateException> { schema.list("ghost") }
        assertTrue(ex2.message!!.contains("Missing required list at ghost"))
    }
}
