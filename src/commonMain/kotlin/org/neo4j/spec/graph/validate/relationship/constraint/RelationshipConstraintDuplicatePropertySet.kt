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
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.constraint.duplicateConstraintIds
import org.neo4j.spec.graph.validate.relationship.RelationshipValidation

object RelationshipConstraintDuplicatePropertySet : RelationshipValidation {
    override fun validateConstraint(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        constraintId: String,
        constraint: RelationshipConstraint,
        issues: MutableList<Issue>
    ) {
        val duplicates = duplicateConstraintIds(constraintId, relationship.constraints)
        if (duplicates.isNotEmpty()) {
            val properties = constraint.properties.joinToString()
            issues.add(
                Issue(
                    code = "duplicate_relation_constraint_property_set",
                    message = "Constraint '$constraintId' (${constraint.type} on $properties) " +
                        "on relationship '$relationshipId' duplicates ${duplicates.joinToString { "'$it'" }}",
                    path = "relationships.$relationshipId.constraints.$constraintId.properties"
                )
            )
        }
    }
}
