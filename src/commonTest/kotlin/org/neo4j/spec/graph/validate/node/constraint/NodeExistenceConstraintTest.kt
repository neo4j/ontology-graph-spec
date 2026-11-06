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
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.node.NodeConstraint
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeExistenceConstraintTest {

    private val validator = NodeExistenceConstraint
    private val model =
        GraphModel(version = "1", id = "test")
    private val nodeDummy = Node()

    @Test
    fun `ignore constraint when type is not EXISTS`() {
        val constraint = NodeConstraint(
            type = ConstraintType.UNIQUE,
            properties = mutableListOf("email", "username") // 2 properties (would fail if it were EXISTS)
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "user", nodeDummy, "uniq_email", constraint, issues)

        assertTrue(issues.isEmpty(), "Expected no issues because constraint type is not EXISTS")
    }

    @Test
    fun `pass when EXISTS constraint has exactly one property`() {
        val constraint = NodeConstraint(
            type = ConstraintType.EXISTS,
            properties = mutableListOf("email")
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "user", nodeDummy, "exist_email", constraint, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when EXISTS constraint has exactly 1 property")
    }

    @Test
    fun `repeating the same property is not counted as multiple properties`() {
        val constraint = NodeConstraint(
            type = ConstraintType.EXISTS,
            properties = mutableListOf("email", "email")
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "user", nodeDummy, "exist_email", constraint, issues)

        assertTrue(issues.isEmpty(), "Duplicate properties are reported by the duplicate property validation")
    }

    @Test
    fun `fail when EXISTS constraint has multiple properties`() {
        val constraint = NodeConstraint(
            type = ConstraintType.EXISTS,
            properties = mutableListOf("email", "username")
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "user", nodeDummy, "exist_fields", constraint, issues)

        assertEquals(1, issues.size)
        val issue = issues.first()
        assertEquals("invalid_node_exist_constraint_property_count", issue.code)
        assertEquals("nodes.user.constraints.exist_fields.properties", issue.path)
    }

    @Test
    fun `fail when EXISTS constraint has zero properties`() {
        val constraint = NodeConstraint(
            type = ConstraintType.EXISTS,
            properties = mutableListOf()
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "user", nodeDummy, "exist_empty", constraint, issues)

        assertEquals(1, issues.size)
        assertEquals("invalid_node_exist_constraint_property_count", issues.first().code)
    }
}
