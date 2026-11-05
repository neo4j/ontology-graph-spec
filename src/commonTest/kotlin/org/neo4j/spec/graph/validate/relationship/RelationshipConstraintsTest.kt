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
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.model.relationship.RelationshipConstraint
import org.neo4j.spec.graph.model.relationship.RelationshipTarget
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationshipConstraintsTest {

    private val validator = RelationshipConstraints
    private val model =
        GraphModel(version = "1", id = "test")
    private val targetDummy = RelationshipTarget()

    @Test
    fun `all constraint properties exist`() {
        val relationshipId = "actedIn"
        val constraintId = "const_roles"

        val relationship = Relationship(
            type = "ACTED_IN",
            from = targetDummy,
            to = targetDummy,
            properties = mutableMapOf(
                "roles" to Property(),
                "year" to Property()
            )
        )
        val constraint = RelationshipConstraint(
            type = ConstraintType.UNIQUE,
            properties = mutableListOf("roles", "year")
        )

        val issues = mutableListOf<Issue>()
        validator.validateConstraint(model, relationshipId, relationship, constraintId, constraint, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when all constraint properties are present")
    }

    @Test
    fun `missing constraint property`() {
        val relationshipId = "actedIn"
        val constraintId = "const_roles"

        val relationship = Relationship(
            type = "ACTED_IN",
            from = targetDummy,
            to = targetDummy,
            properties = mutableMapOf(
                "year" to Property() // Missing 'roles'
            )
        )
        val constraint = RelationshipConstraint(
            type = ConstraintType.UNIQUE,
            properties = mutableListOf("roles")
        )

        val issues = mutableListOf<Issue>()
        validator.validateConstraint(model, relationshipId, relationship, constraintId, constraint, issues)

        assertEquals(1, issues.size)
        val issue = issues.first()
        assertEquals("missing_relation_constraint_property", issue.code)
        assertEquals("Missing property with id 'roles' for relationship constraint 'const_roles'", issue.message)
        assertEquals("relationships.actedIn.constraints.const_roles.properties.roles", issue.path)
    }

    @Test
    fun `missing multiple constraint properties`() {
        val relationshipId = "actedIn"
        val constraintId = "const_compound"

        val relationship = Relationship(
            type = "ACTED_IN",
            from = targetDummy,
            to = targetDummy,
            properties = mutableMapOf() // Missing both 'roles' and 'year'
        )
        val constraint = RelationshipConstraint(
            type = ConstraintType.UNIQUE,
            properties = mutableListOf("roles", "year")
        )

        val issues = mutableListOf<Issue>()
        validator.validateConstraint(model, relationshipId, relationship, constraintId, constraint, issues)

        assertEquals(2, issues.size)

        val missingProperties = issues.map { it.path?.substringAfterLast('.') }.toSet()
        assertEquals(setOf("roles", "year"), missingProperties)
        assertTrue(issues.all { it.code == "missing_relation_constraint_property" })
    }

    @Test
    fun `no properties specified`() {
        val relationshipId = "actedIn"
        val constraintId = "const_empty"

        val relationship = Relationship(
            type = "ACTED_IN",
            from = targetDummy,
            to = targetDummy,
            properties = mutableMapOf("year" to Property())
        )
        val constraint = RelationshipConstraint(
            type = ConstraintType.UNIQUE,
            properties = mutableListOf()
        )

        val issues = mutableListOf<Issue>()
        validator.validateConstraint(model, relationshipId, relationship, constraintId, constraint, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when the constraint does not target any properties")
    }
}
