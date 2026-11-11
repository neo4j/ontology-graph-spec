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
package org.neo4j.spec.graph.validate.node

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.extension.GraphModelExtensions
import org.neo4j.spec.graph.model.extension.mapping.NodeMapping
import org.neo4j.spec.graph.model.extension.source.Table
import org.neo4j.spec.graph.model.node.Labels
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.property.Neo4jType
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodePropertyUnsupportedLocalImportTypeTest {

    private val validator = NodePropertyUnsupportedLocalImportType

    private fun model(propertyType: Neo4jType, vararg tableSources: String): Pair<GraphModel, Node> {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("name" to Property(type = propertyType))
        )
        val tables = mutableMapOf<String, Table>()
        val mappings = mutableListOf<NodeMapping>()
        tableSources.forEachIndexed { index, source ->
            tables["t$index"] = Table(source = source)
            mappings.add(NodeMapping(node = "person", table = "t$index", properties = mutableMapOf()))
        }
        val model = GraphModel(
            version = "1",
            id = "test",
            extensions = GraphModelExtensions(
                dataSourceTables = tables,
                dataSourceMappings = mutableMapOf("mapping0" to mappings.toMutableList())
            )
        )
        return model to node
    }

    private fun validate(model: GraphModel, node: Node): MutableList<Issue> {
        val issues = mutableListOf<Issue>()
        validator.validateNode(model, "person", node, issues)
        return issues
    }

    @Test
    fun `pass when node has no mapping`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("name" to Property(type = Neo4jType.DATE))
        )
        val model = GraphModel(version = "1", id = "test")
        val issues = mutableListOf<Issue>()

        validator.validateNode(model, "person", node, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when node has no mapping")
    }

    @Test
    fun `pass when mapping targets a non-local table`() {
        val (model, node) = model(Neo4jType.DATE, "cloud")

        assertTrue(validate(model, node).isEmpty(), "Expected no issues for non-local tables")
    }

    @Test
    fun `pass when local table and property type is supported`() {
        val (model, node) = model(Neo4jType.STRING, "local")

        assertTrue(validate(model, node).isEmpty())
    }

    @Test
    fun `pass when local table and property type is a supported vector`() {
        val (model, node) = model(Neo4jType.VECTOR_FLOAT, "local")

        assertTrue(validate(model, node).isEmpty())
    }

    @Test
    fun `pass when local table for every supported type`() {
        val supported = listOf(
            Neo4jType.STRING,
            Neo4jType.INTEGER,
            Neo4jType.FLOAT,
            Neo4jType.BOOLEAN,
            Neo4jType.ZONED_DATETIME,
            Neo4jType.LIST_STRING,
            Neo4jType.LIST_INTEGER,
            Neo4jType.LIST_FLOAT,
            Neo4jType.LIST_BOOLEAN,
            Neo4jType.LIST_ZONED_DATETIME,
            Neo4jType.VECTOR_FLOAT,
            Neo4jType.VECTOR_FLOAT32,
            Neo4jType.VECTOR_INTEGER,
            Neo4jType.VECTOR_INTEGER32,
            Neo4jType.VECTOR_INTEGER16,
            Neo4jType.VECTOR_INTEGER8
        )

        val unsupported = supported.filter {
            val (m, n) = model(it, "local")
            validate(m, n).isNotEmpty()
        }

        assertTrue(unsupported.isEmpty(), "Types wrongly flagged: $unsupported")
    }

    @Test
    fun `pass when local table and property type is unset`() {
        val (model, node) = model(Neo4jType.ANY, "local")

        assertTrue(validate(model, node).isEmpty(), "Expected no issues when the type is unset")
    }

    @Test
    fun `fail when local table and property type is unsupported`() {
        val (model, node) = model(Neo4jType.DATE, "local")

        val issues = validate(model, node)

        assertEquals(1, issues.size)
        assertEquals("unsupported_local_import_property_type", issues.first().code)
        assertEquals("nodes.person.properties.name.type", issues.first().path)
    }

    @Test
    fun `fail for each unsupported property`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf(
                "born" to Property(type = Neo4jType.DATE),
                "name" to Property(type = Neo4jType.STRING),
                "home" to Property(type = Neo4jType.POINT)
            )
        )
        val model = GraphModel(
            version = "1",
            id = "test",
            extensions = GraphModelExtensions(
                dataSourceTables = mutableMapOf("t" to Table(source = "local")),
                dataSourceMappings = mutableMapOf(
                    "mapping0" to mutableListOf(
                        NodeMapping(node = "person", table = "t", properties = mutableMapOf())
                    )
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateNode(model, "person", node, issues)

        assertEquals(2, issues.size)
    }

    @Test
    fun `fail once when one of several mappings is local`() {
        val (model, node) = model(Neo4jType.DATE, "cloud", "local")

        val issues = validate(model, node)

        assertEquals(1, issues.size)
    }
}
