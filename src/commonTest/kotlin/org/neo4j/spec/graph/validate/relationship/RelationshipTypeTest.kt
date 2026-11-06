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

class RelationshipTypeTest {

    private val validator = RelationshipType
    private val model =
        GraphModel(version = "1", id = "test")

    @Test
    fun `pass when relationship has a valid type`() {
        val relationship = Relationship(
            type = "ACTED_IN",
            from = RelationshipTarget(),
            to = RelationshipTarget()
        )
        val issues = mutableListOf<Issue>()

        validator.validateRelationship(model, "actedIn", relationship, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when type is set")
    }

    @Test
    fun `fail when relationship has blank type`() {
        val relationship = Relationship(
            type = "  ",
            from = RelationshipTarget(),
            to = RelationshipTarget()
        )
        val issues = mutableListOf<Issue>()

        validator.validateRelationship(model, "blankRel", relationship, issues)

        assertEquals(1, issues.size)
        assertEquals("missing_relationship_type", issues.first().code)
    }
}
