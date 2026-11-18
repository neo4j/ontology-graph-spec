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
// Generated from org/neo4j/spec/graph/validate/node/NodeIndexPropertyDuplicate.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.extension.NodeIndex;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntries;

/**
 * A node index must list each property once.
 */
public final class NodeIndexPropertyDuplicate implements NodeValidation {
    public static final NodeIndexPropertyDuplicate INSTANCE = new NodeIndexPropertyDuplicate();

    private NodeIndexPropertyDuplicate() {}

    @Override
    public void validateIndex(
            GraphModel model, String nodeId, Node node, String indexId, NodeIndex index, List<Issue> issues) {
        issues.addAll(DuplicateEntries.duplicateEntryIssues(
                "duplicate_node_index_property",
                "property",
                "index '" + indexId + "' on node '" + nodeId + "'",
                index.getProperties(),
                "nodes." + nodeId + ".extensions.indexes." + indexId + ".properties"));
    }
}
