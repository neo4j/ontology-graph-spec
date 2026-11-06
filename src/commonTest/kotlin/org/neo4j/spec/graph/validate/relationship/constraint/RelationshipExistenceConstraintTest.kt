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
package org.neo4j.spec.graph.validate.relationship.constraint

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.model.relationship.RelationshipConstraint
import org.neo4j.spec.graph.model.relationship.RelationshipTarget
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationshipExistenceConstraintTest {

    private val validator = RelationshipExistenceConstraint
    private val model =
        GraphModel(version = "1", id = "test")
    private val relationshipDummy = Relationship(
        type = "ACTED_IN",
        from = RelationshipTarget(),
        to = RelationshipTarget()
    )

    @Test
    fun `ignore constraint when type is not EXISTS`() {
        val constraint = RelationshipConstraint(
            type = ConstraintType.UNIQUE,
            properties = mutableListOf("roles", "year")
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "actedIn", relationshipDummy, "uniq_roles", constraint, issues)

        assertTrue(issues.isEmpty(), "Expected no issues because constraint type is not EXISTS")
    }

    @Test
    fun `pass when EXISTS constraint has exactly one property`() {
        val constraint = RelationshipConstraint(
            type = ConstraintType.EXISTS,
            properties = mutableListOf("roles")
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "actedIn", relationshipDummy, "exist_roles", constraint, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when EXISTS constraint has exactly 1 property")
    }

    @Test
    fun `fail when EXISTS constraint has multiple properties`() {
        val constraint = RelationshipConstraint(
            type = ConstraintType.EXISTS,
            properties = mutableListOf("roles", "year")
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "actedIn", relationshipDummy, "exist_multi", constraint, issues)

        assertEquals(1, issues.size)
        val issue = issues.first()
        assertEquals("invalid_relationship_exist_constraint_property_count", issue.code)
        assertEquals("relationships.actedIn.constraints.exist_multi.properties", issue.path)
    }

    @Test
    fun `fail when EXISTS constraint has zero properties`() {
        val constraint = RelationshipConstraint(
            type = ConstraintType.EXISTS,
            properties = mutableListOf()
        )
        val issues = mutableListOf<Issue>()

        validator.validateConstraint(model, "actedIn", relationshipDummy, "exist_empty", constraint, issues)

        assertEquals(1, issues.size)
        assertEquals("invalid_relationship_exist_constraint_property_count", issues.first().code)
    }
}
