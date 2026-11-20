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

class RelationshipTargetCountTest {

    private fun validate(from: RelationshipTarget, to: RelationshipTarget = RelationshipTarget()): List<Issue> {
        val issues = mutableListOf<Issue>()
        RelationshipTargetCount.validateRelationship(
            GraphModel(version = "1", id = "test"),
            "rel",
            Relationship(type = "REL", from = from, to = to),
            issues
        )
        return issues
    }

    @Test
    fun `defaults are valid`() {
        assertTrue(validate(RelationshipTarget()).isEmpty())
    }

    @Test
    fun `exact count only is valid`() {
        assertTrue(validate(RelationshipTarget(count = 3)).isEmpty())
    }

    @Test
    fun `min and max only is valid`() {
        assertTrue(validate(RelationshipTarget(minCount = 1, maxCount = 5)).isEmpty())
    }

    @Test
    fun `count with min is invalid`() {
        val issues = validate(RelationshipTarget(count = 3, minCount = 1))
        assertEquals(listOf("relationship_count_conflict"), issues.map { it.code })
        assertEquals("/relationships/rel/from", issues.single().path)
    }

    @Test
    fun `count with max is invalid on the to side`() {
        val issues = validate(RelationshipTarget(), RelationshipTarget(count = 3, maxCount = 10))
        assertEquals(listOf("relationship_count_conflict"), issues.map { it.code })
        assertEquals("/relationships/rel/to", issues.single().path)
    }

    @Test
    fun `negative values are invalid`() {
        val issues = validate(RelationshipTarget(count = -2, minCount = -1))
        assertTrue("relationship_count_conflict" in issues.map { it.code })
        assertTrue("relationship_min_count_negative" in issues.map { it.code })
    }

    @Test
    fun `negative exact count is invalid`() {
        val issues = validate(RelationshipTarget(count = -1))
        assertEquals(listOf("relationship_count_negative"), issues.map { it.code })
    }

    @Test
    fun `min without max is valid`() {
        assertTrue(validate(RelationshipTarget(minCount = 2)).isEmpty())
    }

    @Test
    fun `min greater than max is invalid`() {
        val issues = validate(RelationshipTarget(minCount = 5, maxCount = 2))
        assertEquals(listOf("relationship_min_count_exceeds_max"), issues.map { it.code })
    }
}
