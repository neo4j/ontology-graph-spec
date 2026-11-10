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
import org.neo4j.spec.graph.validate.IssueKeyType
import org.neo4j.spec.graph.validate.issueDetails
import org.neo4j.spec.graph.validate.mapping.hasStructuredDataSource

object RelationshipEndpointMapping : RelationshipValidation {
    override fun validateRelationship(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        issues: MutableList<Issue>
    ) {
        // UPX: source/target mapping errors (errors.ts#L656-L671, L705-L712), structured sources only.
        if (!model.hasStructuredDataSource()) return
        val mappings = findRelationshipMappings(model, relationshipId)
        if (mappings.isEmpty()) {
            issues.add(missingEndpoint("missing_relationship_source_mapping", relationshipId, "source", null))
            issues.add(missingEndpoint("missing_relationship_target_mapping", relationshipId, "target", null))
            return
        }
        for ((mappingId, index, mapping) in mappings) {
            if (mapping.fromNode.properties.isEmpty()) {
                issues.add(
                    missingEndpoint(
                        "missing_relationship_source_mapping",
                        relationshipId,
                        "source",
                        "extensions.data_source_mappings.$mappingId[$index].from_node"
                    )
                )
            }
            if (mapping.toNode.properties.isEmpty()) {
                issues.add(
                    missingEndpoint(
                        "missing_relationship_target_mapping",
                        relationshipId,
                        "target",
                        "extensions.data_source_mappings.$mappingId[$index].to_node"
                    )
                )
            }
        }
    }

    private fun missingEndpoint(code: String, relationshipId: String, end: String, path: String?) = Issue(
        code = code,
        message = "Relationship '$relationshipId' mapping has no $end node mapping",
        path = path ?: "relationships.$relationshipId",
        details = issueDetails(IssueKeyType.RELATIONSHIP, relationshipId)
    )
}
