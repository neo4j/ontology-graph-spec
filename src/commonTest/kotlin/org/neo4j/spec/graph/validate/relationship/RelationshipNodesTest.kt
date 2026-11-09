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
package org.neo4j.spec.graph.validate.relationship

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.model.relationship.RelationshipTarget
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationshipNodesTest {

    private val validator = RelationshipNodes

    @Test
    fun `both source and target nodes exist`() {
        val model = GraphModel(
            version = "1",
            id = "test"
        ).apply {
            nodes["Person"] = Node()
            nodes["Movie"] = Node()
        }
        val relationship = Relationship(
            type = "ACTED_IN",
            from = RelationshipTarget("Person"),
            to = RelationshipTarget("Movie")
        )
        val issues = mutableListOf<Issue>()

        validator.validateRelationship(model, "actedIn", relationship, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when both nodes are defined in the model")
    }

    @Test
    fun `source and target node identifiers are blank`() {
        val model = GraphModel(version = "1", id = "test")
        val relationship = Relationship(
            type = "ACTED_IN",
            from = RelationshipTarget(""),
            to = RelationshipTarget("   ")
        )
        val issues = mutableListOf<Issue>()

        validator.validateRelationship(model, "actedIn", relationship, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when target/source values are blank")
    }

    @Test
    fun `from node does not exist`() {
        val model = GraphModel(
            version = "1",
            id = "test"
        ).apply {
            nodes["Movie"] = Node() // Only 'to' node exists
        }
        val relationship = Relationship(
            type = "ACTED_IN",
            from = RelationshipTarget("Person"),
            to = RelationshipTarget("Movie")
        )
        val issues = mutableListOf<Issue>()

        validator.validateRelationship(model, "actedIn", relationship, issues)

        assertEquals(1, issues.size)
        val issue = issues.first()
        assertEquals("missing_relationship_from_node", issue.code)
        assertEquals("Missing node with id 'Person' for relationship 'actedIn'", issue.message)
        assertEquals("/relationships/actedIn/from/node", issue.path)
    }

    @Test
    fun `to node does not exist`() {
        val model = GraphModel(
            version = "1",
            id = "test"
        ).apply {
            nodes["Person"] = Node() // Only 'from' node exists
        }
        val relationship = Relationship(
            type = "ACTED_IN",
            from = RelationshipTarget("Person"),
            to = RelationshipTarget("Movie")
        )
        val issues = mutableListOf<Issue>()

        validator.validateRelationship(model, "actedIn", relationship, issues)

        assertEquals(1, issues.size)
        val issue = issues.first()
        assertEquals("missing_relationship_to_node", issue.code)

        assertEquals("Missing node with id 'Movie' for relationship 'actedIn'", issue.message)
        assertEquals("/relationships/actedIn/to/node", issue.path)
    }

    @Test
    fun `neither node exists`() {
        val model = GraphModel(version = "1", id = "test")
        val relationship = Relationship(
            type = "ACTED_IN",
            from = RelationshipTarget("Person"),
            to = RelationshipTarget("Movie")
        )
        val issues = mutableListOf<Issue>()

        validator.validateRelationship(model, "actedIn", relationship, issues)

        assertEquals(2, issues.size)
        assertEquals("missing_relationship_from_node", issues[0].code)
        assertEquals("missing_relationship_to_node", issues[1].code)
    }
}
