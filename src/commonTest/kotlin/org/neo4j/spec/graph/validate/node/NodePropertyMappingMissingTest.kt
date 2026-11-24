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
import org.neo4j.spec.graph.model.extension.mapping.PropertyMapping
import org.neo4j.spec.graph.model.extension.source.Table
import org.neo4j.spec.graph.model.node.Labels
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodePropertyMappingMissingTest {

    private val validator = NodePropertyMappingMissing

    private fun node() = Node(
        labels = Labels(identifier = "Person"),
        properties = mutableMapOf("name" to Property(), "age" to Property())
    )

    private fun modelWith(mappings: List<NodeMapping>) = GraphModel(
        version = "1",
        id = "test",
        extensions = GraphModelExtensions(
            dataSourceTables = mutableMapOf("t" to Table(source = "local")),
            dataSourceMappings = mutableMapOf("mapping0" to mappings.toMutableList())
        )
    )

    private fun validate(model: GraphModel): MutableList<Issue> {
        val issues = mutableListOf<Issue>()
        validator.validateNode(model, "person", node(), issues)
        return issues
    }

    @Test
    fun `pass when model has no data source`() {
        val model = GraphModel(version = "1", id = "test")

        assertTrue(validate(model).isEmpty(), "Expected no issues for unstructured models")
    }

    @Test
    fun `pass when the mapping covers every property`() {
        val model = modelWith(
            listOf(
                NodeMapping(
                    node = "person",
                    table = "t",
                    properties = mutableMapOf("name" to PropertyMapping("name"), "age" to PropertyMapping("age"))
                )
            )
        )

        assertTrue(validate(model).isEmpty())
    }

    @Test
    fun `pass when a second mapping covers the property`() {
        val model = modelWith(
            listOf(
                NodeMapping(
                    node = "person",
                    table = "t",
                    properties = mutableMapOf("name" to PropertyMapping("name"))
                ),
                NodeMapping(
                    node = "person",
                    table = "t",
                    properties = mutableMapOf("age" to PropertyMapping("age"))
                )
            )
        )

        assertTrue(validate(model).isEmpty())
    }

    @Test
    fun `fail when a property is covered by no mapping`() {
        val model = modelWith(
            listOf(
                NodeMapping(
                    node = "person",
                    table = "t",
                    properties = mutableMapOf("name" to PropertyMapping("name"))
                )
            )
        )

        val issues = validate(model)

        assertEquals(1, issues.size)
        assertEquals("missing_property_mapping", issues.first().code)
        assertEquals("/nodes/person/properties/age", issues.first().path)
    }

    @Test
    fun `fail for every property when the node has no mapping`() {
        val model = GraphModel(
            version = "1",
            id = "test",
            extensions = GraphModelExtensions(
                dataSourceTables = mutableMapOf("t" to Table(source = "local"))
            )
        )

        assertEquals(2, validate(model).size)
    }
}
