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
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.model.relationship.RelationshipTarget
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationshipTargetNodesTest {

    private val model = GraphModel(version = "1", id = "test")

    private fun validate(from: String, to: String): List<Issue> {
        val issues = mutableListOf<Issue>()
        val relationship = Relationship(type = "ACTED_IN", from = RelationshipTarget(from), to = RelationshipTarget(to))
        RelationshipTargetNodes.validateRelationship(model, "actedIn", relationship, issues)
        return issues
    }

    @Test
    fun `both nodes set`() {
        assertTrue(validate("Person", "Movie").isEmpty())
    }

    @Test
    fun `from node blank`() {
        val issues = validate("", "Movie")
        assertEquals(1, issues.size)
        assertEquals("missing_relationship_from", issues.first().code)
        assertEquals("/relationships/actedIn/from/node", issues.first().path)
    }

    @Test
    fun `to node blank`() {
        val issues = validate("Person", "  ")
        assertEquals(1, issues.size)
        assertEquals("missing_relationship_to", issues.first().code)
        assertEquals("/relationships/actedIn/to/node", issues.first().path)
    }

    @Test
    fun `both nodes blank`() {
        assertEquals(listOf("missing_relationship_from", "missing_relationship_to"), validate("", "").map { it.code })
    }
}
