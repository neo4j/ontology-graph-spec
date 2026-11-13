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
import org.neo4j.spec.graph.model.extension.mapping.TargetMapping
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.mapping.hasStructuredDataSource
import org.neo4j.spec.graph.validate.node.findNodeMappings

object RelationshipEndpointMapping : RelationshipValidation {
    override fun validateRelationship(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        issues: MutableList<Issue>
    ) {
        if (!model.hasStructuredDataSource()) return
        val mappings = findRelationshipMappings(model, relationshipId)
        if (mappings.isEmpty()) {
            issues.add(missingEndpoint("missing_relationship_source_mapping", relationshipId, "source", null))
            issues.add(missingEndpoint("missing_relationship_target_mapping", relationshipId, "target", null))
            return
        }
        for ((mappingId, index, mapping) in mappings) {
            val base = "extensions.data_source_mappings.$mappingId[$index]"
            validateEndpoint(
                model,
                relationshipId,
                relationship.from.node,
                "source",
                mapping.fromNode,
                "$base.from_node",
                issues
            )
            validateEndpoint(
                model,
                relationshipId,
                relationship.to.node,
                "target",
                mapping.toNode,
                "$base.to_node",
                issues
            )
        }
    }

    private fun validateEndpoint(
        model: GraphModel,
        relationshipId: String,
        nodeId: String,
        end: String,
        target: TargetMapping,
        path: String,
        issues: MutableList<Issue>
    ) {
        val code = "missing_relationship_${end}_mapping"
        if (target.properties.isEmpty()) {
            issues.add(missingEndpoint(code, relationshipId, end, path))
            return
        }
        val keys = findNodeMappings(model, nodeId).map { it.mapping.key }.filter { it.isNotEmpty() }
        if (keys.isNotEmpty() && keys.none { key -> key.all { it in target.properties } }) {
            issues.add(
                Issue(
                    code = code,
                    message = "Relationship '$relationshipId' $end mapping does not cover a key of node '$nodeId'",
                    path = "$path.properties"
                )
            )
        }
    }

    private fun missingEndpoint(code: String, relationshipId: String, end: String, path: String?) = Issue(
        code = code,
        message = "Relationship '$relationshipId' mapping has no $end node mapping",
        path = path ?: "relationships.$relationshipId"
    )
}
