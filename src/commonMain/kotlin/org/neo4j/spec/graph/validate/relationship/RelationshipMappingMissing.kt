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
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.issueDetails
import org.neo4j.spec.graph.validate.mapping.hasStructuredDataSource
import kotlin.js.JsExport

@JsExport
object RelationshipMappingMissing : RelationshipValidation {
    override fun validateRelationship(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        issues: MutableList<Issue>
    ) {
        // UPX: tableSchemaError when a relationship has no mapping (errors.ts#L707), structured sources only.
        if (!model.hasStructuredDataSource()) return
        if (findRelationshipMappings(model, relationshipId).isEmpty()) {
            issues.add(
                Issue(
                    code = "missing_relationship_mapping",
                    message = "Relationship '$relationshipId' has no data source mapping",
                    path = "relationships.$relationshipId",
                    details = issueDetails("relationship", relationshipId)
                )
            )
        }
    }
}
