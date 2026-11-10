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
import org.neo4j.spec.graph.validate.IssueKeyType
import org.neo4j.spec.graph.validate.duplicate.duplicateEntryIssues
import org.neo4j.spec.graph.validate.issueDetails
import org.neo4j.spec.graph.validate.relationship.RelationshipValidation

/**
 * A relationship constraint must list each property once.
 */
object RelationshipConstraintPropertyDuplicate : RelationshipValidation {
    override fun validateConstraint(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        constraintId: String,
        constraint: RelationshipConstraint,
        issues: MutableList<Issue>
    ) {
        issues += duplicateEntryIssues(
            code = "duplicate_relationship_constraint_property",
            kind = "property",
            owner = "constraint '$constraintId' on relationship '$relationshipId'",
            values = constraint.properties,
            path = "relationships.$relationshipId.constraints.$constraintId.properties",
            details = issueDetails(IssueKeyType.CONSTRAINT, constraintId)
        )
    }
}
