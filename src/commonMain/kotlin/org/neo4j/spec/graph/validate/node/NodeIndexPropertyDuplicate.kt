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
import org.neo4j.spec.graph.model.node.extension.NodeIndex
import org.neo4j.spec.graph.validate.Issue
import org.neo4j.spec.graph.validate.duplicate.duplicateEntryIssues

/**
 * A node index must list each property once.
 */
object NodeIndexPropertyDuplicate : NodeValidation {
    override fun validateIndex(
        model: GraphModel,
        nodeId: String,
        node: Node,
        indexId: String,
        index: NodeIndex,
        issues: MutableList<Issue>
    ) {
        issues += duplicateEntryIssues(
            code = "duplicate_node_index_property",
            kind = "property",
            owner = "index '$indexId' on node '$nodeId'",
            values = index.properties,
            path = "nodes.$nodeId.extensions.indexes.$indexId.properties"
        )
    }
}
