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
import org.neo4j.spec.graph.model.extension.source.Table
import org.neo4j.spec.graph.model.property.Neo4jType
import org.neo4j.spec.graph.model.relationship.Relationship
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.IssueKeyType
import org.neo4j.spec.graph.validate.issueDetails
import org.neo4j.spec.graph.validate.node.findNodeMappings

object RelationshipEndpointColumnType : RelationshipValidation {
    override fun validateRelationship(
        model: GraphModel,
        relationshipId: String,
        relationship: Relationship,
        issues: MutableList<Issue>
    ) {
        for ((mappingId, index, mapping) in findRelationshipMappings(model, relationshipId)) {
            val table = model.extensions.dataSourceTables[mapping.table] ?: continue
            val base = "extensions.data_source_mappings.$mappingId[$index]"
            validateEndpoint(
                model,
                relationshipId,
                relationship.from.node,
                mapping.table,
                table,
                mapping.fromNode,
                "$base.from_node",
                issues
            )
            validateEndpoint(
                model,
                relationshipId,
                relationship.to.node,
                mapping.table,
                table,
                mapping.toNode,
                "$base.to_node",
                issues
            )
        }
    }

    // (columns with supported types) on both sides are checked; unmapped key properties and unknown
    // columns are skipped.
    private fun validateEndpoint(
        model: GraphModel,
        relationshipId: String,
        nodeId: String,
        tableId: String,
        table: Table,
        target: TargetMapping,
        endPath: String,
        issues: MutableList<Issue>
    ) {
        val node = model.nodes[nodeId] ?: return
        for ((propertyId, columnMapping) in target.properties) {
            val property = node.properties[propertyId] ?: continue
            if (property.type == Neo4jType.ANY) continue
            val column = table.columns.find { it.name == columnMapping.column } ?: continue
            if (column.supported.isEmpty()) continue
            val cloudSourced = findNodeMappings(model, nodeId)
                .filter { propertyId in it.mapping.properties }
                .any { nodeMapping ->
                    val nodeTable = model.extensions.dataSourceTables[nodeMapping.mapping.table]
                    nodeTable?.columns
                        ?.find { it.name == nodeMapping.mapping.properties[propertyId]?.column }
                        ?.supported?.isNotEmpty() == true
                }
            if (!cloudSourced) continue
            if (property.type !in column.supported) {
                issues.add(
                    Issue(
                        code = "incompatible_relationship_endpoint_column_type",
                        message = "The field '${column.name}' (raw type: ${column.type}, supported Neo4j " +
                            "type(s): ${column.supported.joinToString(", ") { Neo4jType.toString(it) }}) of " +
                            "table '$tableId' cannot be converted to the '$propertyId' " +
                            "(${Neo4jType.toString(property.type)}) property on the '${node.labels.identifier}' " +
                            "node.",
                        path = "$endPath.properties.$propertyId",
                        details = issueDetails(IssueKeyType.RELATIONSHIP, relationshipId)
                    )
                )
            }
        }
    }
}
