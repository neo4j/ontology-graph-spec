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
import org.neo4j.spec.graph.model.type.ConstraintType
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.IssueKeyType
import org.neo4j.spec.graph.validate.issueDetails
import org.neo4j.spec.graph.validate.node.NodeValidation

object NodeKeyOverlap : NodeValidation {
    override fun validateNode(model: GraphModel, nodeId: String, node: Node, issues: MutableList<Issue>) {
        val keyPropertySets = node.constraints.values
            .filter { it.type == ConstraintType.KEY }
            .map { it.properties.toSet() }
            .toSet()

        if (keyPropertySets.isEmpty()) return

        for ((constraintId, constraint) in node.constraints) {
            if (constraint.type != ConstraintType.UNIQUE && constraint.type != ConstraintType.EXISTS) continue

            if (constraint.properties.toSet() in keyPropertySets) {
                issues.add(
                    Issue(
                        code = "redundant_node_constraint_key_overlap",
                        message = "Constraint '$constraintId' (${constraint.type}) is redundant - " +
                            "a KEY constraint already covers the same properties on node '$nodeId'",
                        path = "nodes.$nodeId.constraints.$constraintId",
                        details = issueDetails(IssueKeyType.CONSTRAINT, constraintId)
                    )
                )
            }
        }
    }
}
