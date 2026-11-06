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

class NodeExistenceCompositeConflictTest {

    private val validator = NodeExistenceCompositeConflict
    private val model =
        GraphModel(version = "1", id = "test")

    @Test
    fun `pass when existence does not conflict with composite`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("email" to Property(), "name" to Property()),
            constraints = mutableMapOf(
                "exist_email" to NodeConstraint(
                    type = ConstraintType.EXISTS,
                    properties = mutableListOf("email")
                ),
                "key_name" to NodeConstraint(
                    type = ConstraintType.KEY,
                    properties = mutableListOf("name")
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateNode(model, "personNode", node, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when existence does not conflict")
    }

    @Test
    fun `fail when existence property is in a composite constraint`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("email" to Property(), "name" to Property()),
            constraints = mutableMapOf(
                "exist_email" to NodeConstraint(
                    type = ConstraintType.EXISTS,
                    properties = mutableListOf("email")
                ),
                "comp_email_name" to NodeConstraint(
                    type = ConstraintType.KEY,
                    properties = mutableListOf("email", "name")
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateNode(model, "personNode", node, issues)

        assertEquals(1, issues.size)
        assertEquals("node_existence_composite_conflict", issues.first().code)
    }

    @Test
    fun `pass when other constraint has only 1 property`() {
        // UPX: other constraint must have properties.length >= 2 to conflict
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("email" to Property()),
            constraints = mutableMapOf(
                "exist_email" to NodeConstraint(
                    type = ConstraintType.EXISTS,
                    properties = mutableListOf("email")
                ),
                "key_email" to NodeConstraint(
                    type = ConstraintType.KEY,
                    properties = mutableListOf("email")
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateNode(model, "personNode", node, issues)

        assertTrue(issues.isEmpty(), "1-prop constraint is not composite, no conflict")
    }

    @Test
    fun `pass when other constraint is also existence type`() {
        // UPX: otherConstraint.constraintType !== 'propertyExistence' required
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("email" to Property(), "name" to Property()),
            constraints = mutableMapOf(
                "exist_email" to NodeConstraint(
                    type = ConstraintType.EXISTS,
                    properties = mutableListOf("email")
                ),
                "exist_email_name" to NodeConstraint(
                    type = ConstraintType.EXISTS,
                    properties = mutableListOf("email", "name")
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateNode(model, "personNode", node, issues)

        assertTrue(issues.isEmpty(), "Existence vs existence is not a composite conflict")
    }
}
