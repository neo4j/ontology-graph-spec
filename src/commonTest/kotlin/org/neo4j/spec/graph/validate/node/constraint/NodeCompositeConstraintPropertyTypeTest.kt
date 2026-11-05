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
import org.neo4j.spec.graph.model.property.Neo4jType
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeCompositeConstraintPropertyTypeTest {

    private val validator = NodeCompositeConstraintPropertyType
    private val model =
        GraphModel(version = "1", id = "test")

    @Test
    fun `pass when composite properties are STRING and INTEGER`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf(
                "email" to Property(type = Neo4jType.STRING),
                "age" to Property(type = Neo4jType.INTEGER)
            ),
            constraints = mutableMapOf(
                "comp" to NodeConstraint(
                    type = ConstraintType.KEY,
                    properties = mutableListOf("email", "age")
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "personNode", node, "comp", node.constraints["comp"]!!, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when composite properties are STRING/INTEGER")
    }

    @Test
    fun `fail when composite property is FLOAT`() {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf(
                "email" to Property(type = Neo4jType.STRING),
                "score" to Property(type = Neo4jType.FLOAT)
            ),
            constraints = mutableMapOf(
                "comp" to NodeConstraint(
                    type = ConstraintType.KEY,
                    properties = mutableListOf("email", "score")
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "personNode", node, "comp", node.constraints["comp"]!!, issues)

        assertEquals(1, issues.size)
        assertEquals("invalid_node_composite_constraint_property_type", issues.first().code)
    }

    @Test
    fun `fail once per constraint when multiple properties are invalid`() {
        // the subject of the error is the constraint, not the properties
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf(
                "score" to Property(type = Neo4jType.FLOAT),
                "rating" to Property(type = Neo4jType.FLOAT)
            ),
            constraints = mutableMapOf(
                "comp" to NodeConstraint(
                    type = ConstraintType.KEY,
                    properties = mutableListOf("score", "rating")
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "personNode", node, "comp", node.constraints["comp"]!!, issues)

        // one issue per constraint, not per property
        assertEquals(1, issues.size)
        assertEquals("invalid_node_composite_constraint_property_type", issues.first().code)
        assertEquals("nodes.personNode.constraints.comp", issues.first().path)
    }

    @Test
    fun `pass when 1 property no name constraint has invalid type - draft composites are not type checked`() {
        // UPX: getBulkImportCompositeConstraintErrors filters properties.length >= 2 only
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("score" to Property(type = Neo4jType.FLOAT)),
            constraints = mutableMapOf(
                "comp" to NodeConstraint(
                    type = ConstraintType.KEY,
                    properties = mutableListOf("score")
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "personNode", node, "comp", node.constraints["comp"]!!, issues)

        assertTrue(
            issues.isEmpty(),
            "Expected no issues - draft composites (1 prop, no name) are not type checked in UPX"
        )
    }

    @Test
    fun `fail when composite property is a list type`() {
        // UPX: Array.isArray(property.type) is invalid even for LIST<STRING>
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf(
                "email" to Property(type = Neo4jType.STRING),
                "tags" to Property(type = Neo4jType.LIST_STRING)
            ),
            constraints = mutableMapOf(
                "comp" to NodeConstraint(
                    type = ConstraintType.KEY,
                    properties = mutableListOf("email", "tags")
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "personNode", node, "comp", node.constraints["comp"]!!, issues)

        assertEquals(1, issues.size)
        assertEquals("invalid_node_composite_constraint_property_type", issues.first().code)
    }

    @Test
    fun `pass when composite references a missing property - skipped like UPX`() {
        // UPX logs an error and treats the missing ref as not invalid
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("email" to Property(type = Neo4jType.STRING)),
            constraints = mutableMapOf(
                "comp" to NodeConstraint(
                    type = ConstraintType.KEY,
                    properties = mutableListOf("email", "missing")
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "personNode", node, "comp", node.constraints["comp"]!!, issues)

        assertTrue(issues.isEmpty(), "Expected no issues - missing property refs are skipped like UPX")
    }
}
