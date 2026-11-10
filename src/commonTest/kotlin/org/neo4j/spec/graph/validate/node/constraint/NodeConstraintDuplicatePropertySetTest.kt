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
import org.neo4j.spec.graph.validate.IssueKeyType
import org.neo4j.spec.graph.validate.issueDetails
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeConstraintDuplicatePropertySetTest {

    private fun validate(constraints: Map<String, NodeConstraint>): List<Issue> {
        val node = Node(
            labels = Labels(identifier = "Person"),
            properties = mutableMapOf("email" to Property(), "name" to Property()),
            constraints = constraints.toMutableMap()
        )
        val model = GraphModel(version = "1", id = "test", nodes = mutableMapOf("person" to node))
        val issues = mutableListOf<Issue>()
        NodeConstraintDuplicatePropertySet.validate(model, issues)
        return issues
    }

    private fun constraint(type: ConstraintType, vararg properties: String) =
        NodeConstraint(type, properties.toMutableList())

    @Test
    fun `pass when single property constraints of different types cover the same property`() {
        val issues = validate(
            mapOf(
                "exists_email" to constraint(ConstraintType.EXISTS, "email"),
                "unique_email" to constraint(ConstraintType.UNIQUE, "email"),
                "type_email" to constraint(ConstraintType.PROPERTY_TYPE, "email")
            )
        )

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `pass when constraints cover different properties`() {
        val issues = validate(
            mapOf(
                "key_email_name" to constraint(ConstraintType.KEY, "email", "name"),
                "unique_email" to constraint(ConstraintType.UNIQUE, "email"),
                "key_name" to constraint(ConstraintType.KEY, "name")
            )
        )

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `fail when two single property constraints of the same type cover the same property`() {
        val issues = validate(
            mapOf(
                "key1" to constraint(ConstraintType.KEY, "email"),
                "key2" to constraint(ConstraintType.KEY, "email")
            )
        )

        assertEquals(
            listOf(
                Issue(
                    code = "duplicate_node_constraint_property_set",
                    message = "Constraint 'key1' (KEY on email) on node 'person' duplicates 'key2'",
                    path = "nodes.person.constraints.key1.properties",
                    details = issueDetails(IssueKeyType.CONSTRAINT, "key1")
                ),
                Issue(
                    code = "duplicate_node_constraint_property_set",
                    message = "Constraint 'key2' (KEY on email) on node 'person' duplicates 'key1'",
                    path = "nodes.person.constraints.key2.properties",
                    details = issueDetails(IssueKeyType.CONSTRAINT, "key2")
                )
            ),
            issues
        )
    }

    @Test
    fun `fail when two composite constraints cover the same properties`() {
        val issues = validate(
            mapOf(
                "key1" to constraint(ConstraintType.KEY, "email", "name"),
                "unique1" to constraint(ConstraintType.UNIQUE, "email", "name")
            )
        )

        assertEquals(
            listOf(
                "Constraint 'key1' (KEY on email, name) on node 'person' duplicates 'unique1'",
                "Constraint 'unique1' (UNIQUE on email, name) on node 'person' duplicates 'key1'"
            ),
            issues.map { it.message }
        )
    }

    @Test
    fun `fail when property sets are equal in different order`() {
        val issues = validate(
            mapOf(
                "key1" to constraint(ConstraintType.KEY, "email", "name"),
                "key2" to constraint(ConstraintType.KEY, "name", "email")
            )
        )

        assertEquals(2, issues.size)
    }

    @Test
    fun `names every duplicate`() {
        val issues = validate(
            mapOf(
                "unique1" to constraint(ConstraintType.UNIQUE, "email"),
                "unique2" to constraint(ConstraintType.UNIQUE, "email"),
                "unique3" to constraint(ConstraintType.UNIQUE, "email")
            )
        )

        assertEquals(3, issues.size)
        assertEquals(
            "Constraint 'unique1' (UNIQUE on email) on node 'person' duplicates 'unique2', 'unique3'",
            issues.first().message
        )
    }
}
