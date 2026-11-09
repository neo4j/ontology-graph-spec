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
package org.neo4j.spec.graph.validate.node

import org.neo4j.spec.graph.model.GraphModel
import org.neo4j.spec.graph.model.node.Node
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.pathOf

/**
 * Index and constraint names must be unique across the whole model. Any name used by more than one
 * index or constraint is a `duplicate_node_index_constraint_name`, attached to every index and constraint
 * carrying that name. Blank and missing names are skipped.
 *
 * Ported from UPX `getNonUniqueNames` (import-shared/src/data-model/errors.ts). Graph-spec supports
 * mapping multiple files or columns to the same node natively, so it does not carry UPX's
 * "identical definition may reuse a name" exception - all duplicate names are flagged.
 */
object NodeIndexConstraintNameConflict : NodeValidation {
    override fun validateNode(model: GraphModel, nodeId: String, node: Node, issues: MutableList<Issue>) {
        val duplicateNames = duplicateNames(model)

        for ((indexId, index) in node.extensions.indexes) {
            val name = index.name
            if (!name.isNullOrEmpty() && name in duplicateNames) {
                issues.add(
                    Issue(
                        code = "duplicate_node_index_constraint_name",
                        message = "Index '$indexId' on node '$nodeId' reuses name '$name' " +
                            "already used by another index or constraint",
                        path = pathOf("nodes", nodeId, "extensions", "indexes", indexId, "name")
                    )
                )
            }
        }

        for ((constraintId, constraint) in node.constraints) {
            val name = constraint.name
            if (!name.isNullOrEmpty() && name in duplicateNames) {
                issues.add(
                    Issue(
                        code = "duplicate_node_index_constraint_name",
                        message = "Constraint '$constraintId' on node '$nodeId' reuses name '$name' " +
                            "already used by another index or constraint",
                        path = pathOf("nodes", nodeId, "constraints", constraintId, "name")
                    )
                )
            }
        }
    }

    private fun duplicateNames(model: GraphModel): Set<String> {
        val seen = mutableMapOf<String, Int>()
        for (node in model.nodes.values) {
            for (index in node.extensions.indexes.values) {
                val name = index.name
                if (!name.isNullOrEmpty()) {
                    seen[name] = (seen[name] ?: 0) + 1
                }
            }
            for (constraint in node.constraints.values) {
                val name = constraint.name
                if (!name.isNullOrEmpty()) {
                    seen[name] = (seen[name] ?: 0) + 1
                }
            }
        }
        return seen.filterValues { it > 1 }.keys
    }
}
