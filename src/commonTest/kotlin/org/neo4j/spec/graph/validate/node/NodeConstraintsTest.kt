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
import org.neo4j.spec.graph.model.node.NodeConstraint
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeConstraintsTest {

    private val model =
        GraphModel(version = "1", id = "test")
    private val validator = NodeConstraints

    @Test
    fun `valid property and identifier label`() {
        val nodeId = "userNode"
        val constraintId = "uniq_user_email"

        val node = Node(
            labels = Labels(identifier = "User"),
            properties = mutableMapOf("email" to Property())
        )
        val constraint = NodeConstraint(
            type = ConstraintType.UNIQUE,
            properties = mutableSetOf("email")
        )

        val issues = mutableListOf<Issue>()
        validator.validateConstraint(model, nodeId, node, constraintId, constraint, issues)

        assertTrue(issues.isEmpty(), "Expected no validation issues, but found: ${issues.size}")
    }

    @Test
    fun `valid property and implied label`() {
        val nodeId = "userNode"
        val constraintId = "uniq_user_email"

        val node = Node(
            labels = Labels(implied = mutableSetOf("Person", "User")),
            properties = mutableMapOf("email" to Property())
        )
        val constraint = NodeConstraint(
            type = ConstraintType.UNIQUE,
            properties = mutableSetOf("email")
        )

        val issues = mutableListOf<Issue>()
        validator.validateConstraint(model, nodeId, node, constraintId, constraint, issues)

        assertTrue(issues.isEmpty(), "Expected no validation issues when label is in implied set")
    }

    @Test
    fun `missing property`() {
        val nodeId = "userNode"
        val constraintId = "uniq_user_email"

        val node = Node(
            labels = Labels(identifier = "User"),
            properties = mutableMapOf() // Missing 'email' property
        )
        val constraint = NodeConstraint(
            type = ConstraintType.UNIQUE,
            properties = mutableSetOf("email")
        )

        val issues = mutableListOf<Issue>()
        validator.validateConstraint(model, nodeId, node, constraintId, constraint, issues)

        assertEquals(1, issues.size)
        val issue = issues.first()
        assertEquals("missing_node_constraint_property", issue.code)
        assertEquals("/nodes/userNode/constraints/uniq_user_email/properties/email", issue.path)
    }

    @Test
    fun `missing properties`() {
        val nodeId = "userNode"
        val constraintId = "uniq_user_email"

        val node = Node(
            labels = Labels(identifier = "IncorrectLabel"),
            properties = mutableMapOf() // Missing 'email' property
        )
        val constraint = NodeConstraint(
            type = ConstraintType.UNIQUE,
            properties = mutableSetOf("email", "name")
        )

        val issues = mutableListOf<Issue>()
        validator.validateConstraint(model, nodeId, node, constraintId, constraint, issues)

        assertEquals(2, issues.size)
        assertEquals("missing_node_constraint_property", issues[0].code)
        assertEquals("missing_node_constraint_property", issues[1].code)
    }
}
