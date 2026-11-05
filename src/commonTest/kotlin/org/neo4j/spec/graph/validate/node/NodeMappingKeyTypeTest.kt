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
import org.neo4j.spec.graph.model.extension.mapping.NodeMapping
import org.neo4j.spec.graph.model.node.Labels
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.property.Neo4jType
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeMappingKeyTypeTest {

    private val validator = NodeMappingKeyType
    private val model =
        GraphModel(version = "1", id = "test")

    @Test
    fun `pass when mapping key is STRING`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("id" to Property(type = Neo4jType.STRING))
        )
        model.nodes["person"] = node
        model.extensions.dataSourceMappings["mapping0"] = mutableListOf(
            NodeMapping(
                node = "person",
                table = "people.csv",
                properties = mutableMapOf(),
                key = mutableListOf("id")
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateNode(model, "person", node, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when mapping key is STRING")
    }

    @Test
    fun `pass when mapping key is INTEGER`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("id" to Property(type = Neo4jType.INTEGER))
        )
        model.nodes["person"] = node
        model.extensions.dataSourceMappings["mapping0"] = mutableListOf(
            NodeMapping(
                node = "person",
                table = "people.csv",
                properties = mutableMapOf(),
                key = mutableListOf("id")
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateNode(model, "person", node, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when mapping key is INTEGER")
    }

    @Test
    fun `fail when mapping key is FLOAT`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("id" to Property(type = Neo4jType.FLOAT))
        )
        model.nodes["person"] = node
        model.extensions.dataSourceMappings["mapping0"] = mutableListOf(
            NodeMapping(
                node = "person",
                table = "people.csv",
                properties = mutableMapOf(),
                key = mutableListOf("id")
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateNode(model, "person", node, issues)

        // Replaced legacy code: invalid_node_key_property_type
        assertEquals(1, issues.size)
        assertEquals("invalid_node_mapping_key_type", issues.first().code)
    }
}
