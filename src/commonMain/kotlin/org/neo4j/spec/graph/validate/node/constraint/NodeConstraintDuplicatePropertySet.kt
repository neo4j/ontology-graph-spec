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
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.constraint.isCompositeConstraint
import org.neo4j.spec.graph.validate.issueDetails
import org.neo4j.spec.graph.validate.node.NodeValidation
import org.neo4j.spec.graph.validate.pathOf
import kotlin.js.JsExport

@JsExport
object NodeConstraintDuplicatePropertySet : NodeValidation {
    override fun validateNode(model: GraphModel, nodeId: String, node: Node, issues: MutableList<Issue>) {
        val compositeConstraints = node.constraints.values.filter { isCompositeConstraint(it.properties) }
        for ((constraintId, constraint) in node.constraints) {
            if (!isCompositeConstraint(constraint.properties)) continue
            val isDuplicate = compositeConstraints.any { other ->
                other !== constraint && other.properties.toSet() == constraint.properties.toSet()
            }
            if (isDuplicate) {
                issues.add(
                    Issue(
                        code = "duplicate_node_constraint_property_set",
                        message = "Constraint '$constraintId' property set duplicates " +
                            "another composite constraint on node '$nodeId'",
                        path = pathOf("nodes", nodeId, "constraints", constraintId, "properties"),
                        details = issueDetails("constraint", constraintId)
                    )
                )
            }
        }
    }
}
