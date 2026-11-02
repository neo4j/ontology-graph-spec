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
import org.neo4j.spec.graph.model.node.Labels
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodePropertyEmptyNameTest {

    private val validator = NodePropertyEmptyName
    private val model =
        GraphModel(version = "1", id = "test")

    @Test
    fun `pass when name is blank and the key is the name`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("p:1" to Property(name = ""))
        )
        val issues = mutableListOf<Issue>()

        validator.validateProperty(model, "n:1", node, "p:1", node.properties["p:1"]!!, issues)

        assertTrue(issues.isEmpty(), "Expected no issues; a blank name override falls back to the key")
    }

    @Test
    fun `pass when name is omitted and the key is the name`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("p:1" to Property(name = null))
        )
        val issues = mutableListOf<Issue>()

        validator.validateProperty(model, "n:1", node, "p:1", node.properties["p:1"]!!, issues)

        assertTrue(issues.isEmpty(), "Expected no issues; the map key is the property name")
    }

    @Test
    fun `fail when the key is blank and no name is set`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("" to Property())
        )
        val issues = mutableListOf<Issue>()

        validator.validateProperty(model, "n:1", node, "", node.properties[""]!!, issues)

        assertEquals(1, issues.size)
        assertEquals("missing_property_name", issues.first().code)
    }

    @Test
    fun `pass when property has a valid name`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("p:1" to Property(name = "email"))
        )
        val issues = mutableListOf<Issue>()

        validator.validateProperty(model, "n:1", node, "p:1", node.properties["p:1"]!!, issues)

        assertTrue(issues.isEmpty())
    }
}
