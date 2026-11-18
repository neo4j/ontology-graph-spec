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
// Generated from org/neo4j/spec/graph/validate/node/NodeConstraintCoverage.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.validate.Issue;

public final class NodeConstraintCoverage implements NodeValidation {
    public static final NodeConstraintCoverage INSTANCE = new NodeConstraintCoverage();

    private NodeConstraintCoverage() {}

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        if (node.getConstraints().isEmpty()) {
            issues.add(new Issue(
                    "missing_node_constraint_coverage",
                    "Node '" + nodeId + "' has no constraints — add a KEY, UNIQUENESS, or EXISTENCE constraint.",
                    "nodes." + nodeId + ".constraints"));
        }
    }
}
