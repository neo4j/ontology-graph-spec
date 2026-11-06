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
package org.neo4j.spec.graph.validate.node.constraint

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.node.Labels
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.node.NodeConstraint
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeConstraintPropertiesTest {

    private val validator = NodeConstraintProperties
    private val model =
        GraphModel(version = "1", id = "test")

    @Test
    fun `pass when constraint has properties`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("email" to Property()),
            constraints = mutableMapOf(
                "uniq_email" to NodeConstraint(
                    type = ConstraintType.UNIQUE,
                    properties = mutableListOf("email")
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "personNode", node, "uniq_email", node.constraints["uniq_email"]!!, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when constraint has properties")
    }

    @Test
    fun `fail when constraint has no properties`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("email" to Property()),
            constraints = mutableMapOf(
                "empty" to NodeConstraint(
                    type = ConstraintType.UNIQUE,
                    properties = mutableListOf()
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "personNode", node, "empty", node.constraints["empty"]!!, issues)

        assertEquals(1, issues.size)
        assertEquals("missing_node_constraint_properties", issues.first().code)
    }
}
