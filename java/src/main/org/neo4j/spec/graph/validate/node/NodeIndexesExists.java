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
import org.neo4j.spec.graph.model.node.extension.NodeIndex;
import org.neo4j.spec.graph.validate.Issue;

public final class NodeIndexesExists implements NodeValidation {
    public static final NodeIndexesExists INSTANCE = new NodeIndexesExists();

    private NodeIndexesExists() {}

    @Override
    public void validateIndex(
            GraphModel model, String nodeId, Node node, String indexId, NodeIndex index, List<Issue> issues) {
        for (int propertyIndex = 0; propertyIndex < index.getProperties().size(); propertyIndex++) {
            var property = index.getProperties().get(propertyIndex);
            if (!node.getProperties().containsKey(property)) {
                issues.add(new Issue(
                        "missing_node_index_property",
                        "Missing property with id '" + property + "' for node index '" + indexId + "'",
                        Issue.jsonPointerOf(
                                "nodes", nodeId, "extensions", "indexes", indexId, "properties", propertyIndex)));
            }
        }
    }
}
