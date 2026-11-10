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
import org.neo4j.spec.graph.validate.constraint.isCompositeConstraint
import org.neo4j.spec.graph.validate.issueDetails
import org.neo4j.spec.graph.validate.node.NodeValidation

object NodeExistenceCompositeConflict : NodeValidation {
    override fun validateNode(model: GraphModel, nodeId: String, node: Node, issues: MutableList<Issue>) {
        val compositeConstraints = node.constraints.values.filter {
            it.type != ConstraintType.EXISTS && isCompositeConstraint(it.properties)
        }
        for ((constraintId, constraint) in node.constraints) {
            if (constraint.type != ConstraintType.EXISTS || constraint.properties.distinct().size != 1) continue
            val propertyId = constraint.properties.first()
            val conflictsWithComposite = compositeConstraints.any { other ->
                other.properties.contains(propertyId)
            }
            if (conflictsWithComposite) {
                issues.add(
                    Issue(
                        code = "node_existence_composite_conflict",
                        message = "Existence constraint '$constraintId' on property " +
                            "'$propertyId' conflicts with a composite constraint on node '$nodeId'",
                        path = "nodes.$nodeId.constraints.$constraintId",
                        details = issueDetails(IssueKeyType.CONSTRAINT, constraintId)
                    )
                )
            }
        }
    }
}
