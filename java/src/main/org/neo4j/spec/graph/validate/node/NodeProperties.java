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

public final class NodeProperties implements NodeValidation {
    public static final NodeProperties INSTANCE = new NodeProperties();

    private NodeProperties() {}

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        if (node.getProperties().isEmpty()) {
            issues.add(new Issue(
                    "missing_node_properties",
                    "Node '" + nodeId + "' has no properties",
                    "nodes." + nodeId + ".properties"));
        }
    }
}
