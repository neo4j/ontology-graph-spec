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
import org.neo4j.spec.graph.model.property.Property
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.model.relationship.RelationshipConstraint
import org.neo4j.spec.graph.model.relationship.RelationshipTarget
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.validate.Issue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelationshipKeyOverlapTest {

    private val validator = RelationshipKeyOverlap
    private val model =
        GraphModel(version = "1", id = "test")
    private val dummyRel = Relationship(
        type = "ACTED_IN",
        from = RelationshipTarget(),
        to = RelationshipTarget()
    )

    @Test
    fun `pass when no KEY constraint exists`() {
        val relationship = dummyRel.copy(
            properties = mutableMapOf("roles" to Property()),
            constraints = mutableMapOf(
                "uniq_roles" to RelationshipConstraint(
                    type = ConstraintType.UNIQUE,
                    properties = mutableListOf("roles")
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateRelationship(model, "actedIn", relationship, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when no KEY constraint exists")
    }

    @Test
    fun `pass when UNIQUENESS covers different properties than KEY`() {
        val relationship = dummyRel.copy(
            properties = mutableMapOf("roles" to Property(), "year" to Property()),
            constraints = mutableMapOf(
                "key_roles" to RelationshipConstraint(
                    type = ConstraintType.KEY,
                    properties = mutableListOf("roles")
                ),
                "uniq_year" to RelationshipConstraint(
                    type = ConstraintType.UNIQUE,
                    properties = mutableListOf("year")
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateRelationship(model, "actedIn", relationship, issues)

        assertTrue(issues.isEmpty(), "Expected no issues when UNIQUENESS covers different properties")
    }

    @Test
    fun `fail when UNIQUENESS is redundant with KEY`() {
        val relationship = dummyRel.copy(
            properties = mutableMapOf("roles" to Property()),
            constraints = mutableMapOf(
                "key_roles" to RelationshipConstraint(
                    type = ConstraintType.KEY,
                    properties = mutableListOf("roles")
                ),
                "uniq_roles" to RelationshipConstraint(
                    type = ConstraintType.UNIQUE,
                    properties = mutableListOf("roles")
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateRelationship(model, "actedIn", relationship, issues)

        assertEquals(1, issues.size)
        assertEquals("redundant_relation_constraint_key_overlap", issues.first().code)
    }

    @Test
    fun `fail when EXISTENCE is redundant with KEY`() {
        val relationship = dummyRel.copy(
            properties = mutableMapOf("roles" to Property()),
            constraints = mutableMapOf(
                "key_roles" to RelationshipConstraint(
                    type = ConstraintType.KEY,
                    properties = mutableListOf("roles")
                ),
                "exist_roles" to RelationshipConstraint(
                    type = ConstraintType.EXISTS,
                    properties = mutableListOf("roles")
                )
            )
        )
        val issues = mutableListOf<Issue>()

        validator.validateRelationship(model, "actedIn", relationship, issues)

        assertEquals(1, issues.size)
        assertEquals("redundant_relation_constraint_key_overlap", issues.first().code)
    }
}
