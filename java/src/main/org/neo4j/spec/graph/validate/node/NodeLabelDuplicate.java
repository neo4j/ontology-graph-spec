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
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntries;

/**
 * A node's implied labels and optional labels must each list a label once.
 */
public final class NodeLabelDuplicate implements NodeValidation {
    public static final NodeLabelDuplicate INSTANCE = new NodeLabelDuplicate();

    private NodeLabelDuplicate() {}

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        issues.addAll(DuplicateEntries.duplicateEntryIssues(
                "duplicate_node_implied_label",
                "implied label",
                "node '" + nodeId + "'",
                node.getLabels().getImplied(),
                "nodes." + nodeId + ".labels.implied"));
        issues.addAll(DuplicateEntries.duplicateEntryIssues(
                "duplicate_node_optional_label",
                "optional label",
                "node '" + nodeId + "'",
                node.getLabels().getOptional(),
                "nodes." + nodeId + ".labels.optional"));
    }
}
