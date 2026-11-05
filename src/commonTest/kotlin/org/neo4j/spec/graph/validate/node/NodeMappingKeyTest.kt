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
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeMappingKeyTest {

    private val validator = NodeMappingKey
    private val model =
        GraphModel(version = "1", id = "test")

    @Test
    fun `pass when node has no mapping`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("name" to Property())
        )
        model.nodes["person"] = node
        val issues = mutableListOf<Issue>()

        validator.validateNode(model, "person", node, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when node has no mapping")
    }

    @Test
    fun `pass when node mapping has key`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("name" to Property())
        )
        model.nodes["person"] = node
        model.extensions.dataSourceMappings["mapping0"] = mutableListOf(
            NodeMapping(
                node = "person",
                table = "people.csv",
                properties = mutableMapOf(),
                key = mutableListOf("name")
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateNode(model, "person", node, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when node mapping has key")
    }

    @Test
    fun `fail when node mapping has no key`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("name" to Property())
        )
        model.nodes["person"] = node
        model.extensions.dataSourceMappings["mapping0"] = mutableListOf(
            NodeMapping(
                node = "person",
                table = "people.csv",
                properties = mutableMapOf(),
                key = mutableListOf()
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateNode(model, "person", node, issues)

        // Replaced legacy code: missing_node_key_property
        assertEquals(1, issues.size)
        assertEquals("missing_node_mapping_key", issues.first().code)
    }

    @Test
    fun `fail when a second mapping of the node has no key`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("name" to Property())
        )
        model.nodes["person"] = node
        model.extensions.dataSourceMappings["mapping0"] = mutableListOf(
            NodeMapping(
                node = "person",
                table = "people.csv",
                properties = mutableMapOf(),
                key = mutableListOf("name")
            ),
            NodeMapping(
                node = "person",
                table = "more_people.csv",
                properties = mutableMapOf(),
                key = mutableListOf()
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateNode(model, "person", node, issues)

        assertEquals(1, issues.size)
        assertEquals("extensions.data_source_mappings.mapping0[1].key", issues[0].path)
    }
}
