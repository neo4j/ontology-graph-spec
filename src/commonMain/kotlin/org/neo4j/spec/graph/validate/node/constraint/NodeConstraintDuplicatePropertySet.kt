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
package org.neo4j.spec.graph.validate.node.constraint

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.model.node.NodeConstraint
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.constraint.duplicateConstraintIds
import org.neo4j.spec.graph.validate.node.NodeValidation
import org.neo4j.spec.graph.validate.pathOf

object NodeConstraintDuplicatePropertySet : NodeValidation {
    override fun validateConstraint(
        model: GraphModel,
        nodeId: String,
        node: Node,
        constraintId: String,
        constraint: NodeConstraint,
        issues: MutableList<Issue>
    ) {
        val duplicates = duplicateConstraintIds(constraintId, node.constraints)
        if (duplicates.isNotEmpty()) {
            val properties = constraint.properties.joinToString()
            issues.add(
                Issue(
                    code = "duplicate_node_constraint_property_set",
                    message = "Constraint '$constraintId' (${constraint.type} on $properties) on node '$nodeId' " +
                        "duplicates ${duplicates.joinToString { "'$it'" }}",
                    path = pathOf("nodes", nodeId, "constraints", constraintId, "properties")
                )
            )
        }
    }
}
