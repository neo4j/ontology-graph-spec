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

class RelationshipConstraintDuplicatePropertySetTest {

    private fun validate(constraints: Map<String, RelationshipConstraint>): List<Issue> {
        val relationship = Relationship(
            type = "ACTED_IN",
            from = RelationshipTarget(),
            to = RelationshipTarget(),
            properties = mutableMapOf("role" to Property(), "since" to Property()),
            constraints = constraints.toMutableMap()
        )
        val model = GraphModel(version = "1", id = "test", relationships = mutableMapOf("actedIn" to relationship))
        val issues = mutableListOf<Issue>()
        RelationshipConstraintDuplicatePropertySet.validate(model, issues)
        return issues
    }

    private fun constraint(type: ConstraintType, vararg properties: String) =
        RelationshipConstraint(type, properties.toMutableList())

    @Test
    fun `pass when constraints cover different properties`() {
        val issues = validate(
            mapOf(
                "key_role_since" to constraint(ConstraintType.KEY, "role", "since"),
                "unique_role" to constraint(ConstraintType.UNIQUE, "role"),
                "exists_role" to constraint(ConstraintType.EXISTS, "role")
            )
        )

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `fail when two single property constraints of the same type cover the same property`() {
        val issues = validate(
            mapOf(
                "exists1" to constraint(ConstraintType.EXISTS, "role"),
                "exists2" to constraint(ConstraintType.EXISTS, "role")
            )
        )

        assertEquals(
            listOf(
                Issue(
                    code = "duplicate_relationship_constraint_property_set",
                    message = "Constraint 'exists1' (EXISTS on role) on relationship 'actedIn' duplicates 'exists2'",
                    path = "/relationships/actedIn/constraints/exists1/properties"
                ),
                Issue(
                    code = "duplicate_relationship_constraint_property_set",
                    message = "Constraint 'exists2' (EXISTS on role) on relationship 'actedIn' duplicates 'exists1'",
                    path = "/relationships/actedIn/constraints/exists2/properties"
                )
            ),
            issues
        )
    }

    @Test
    fun `fail when two composite constraints cover the same properties`() {
        val issues = validate(
            mapOf(
                "key1" to constraint(ConstraintType.KEY, "role", "since"),
                "unique1" to constraint(ConstraintType.UNIQUE, "since", "role")
            )
        )

        assertEquals(2, issues.size)
    }
}
