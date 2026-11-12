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
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.jsonPointerOf
import org.neo4j.spec.graph.validate.relationship.RelationshipValidation

object RelationshipKeyOverlap : RelationshipValidation {
    override fun validateRelationship(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        issues: MutableList<Issue>
    ) {
        val keyPropertySets = relationship.constraints.values
            .filter { it.type == ConstraintType.KEY }
            .map { it.properties.toSet() }
            .toSet()

        if (keyPropertySets.isEmpty()) return

        for ((constraintId, constraint) in relationship.constraints) {
            if (constraint.type != ConstraintType.UNIQUE && constraint.type != ConstraintType.EXISTS) continue

            if (constraint.properties.toSet() in keyPropertySets) {
                issues.add(
                    Issue(
                        code = "redundant_relationship_constraint_key_overlap",
                        message = "Constraint '$constraintId' (${constraint.type}) is redundant - " +
                            "a KEY constraint already covers the same properties on relationship '$relationshipId'",
                        path = jsonPointerOf("relationships", relationshipId, "constraints", constraintId)
                    )
                )
            }
        }
    }
}
