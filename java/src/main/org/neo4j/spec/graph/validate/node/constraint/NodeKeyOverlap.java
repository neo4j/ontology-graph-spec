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
// Generated from org/neo4j/spec/graph/validate/node/constraint/NodeKeyOverlap.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node.constraint;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.node.NodeValidation;

public final class NodeKeyOverlap implements NodeValidation {
    public static final NodeKeyOverlap INSTANCE = new NodeKeyOverlap();

    private NodeKeyOverlap() {}

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        Set<Set<String>> keyPropertySets = new LinkedHashSet<>();
        for (var keyConstraint : node.getConstraints().values()) {
            if (keyConstraint.getType() == ConstraintType.KEY) {
                keyPropertySets.add(new LinkedHashSet<>(keyConstraint.getProperties()));
            }
        }
        if (keyPropertySets.isEmpty()) {
            return;
        }
        for (var entry : node.getConstraints().entrySet()) {
            var constraintId = entry.getKey();
            var constraint = entry.getValue();
            if (constraint.getType() != ConstraintType.UNIQUE && constraint.getType() != ConstraintType.EXISTS) {
                continue;
            }
            if (keyPropertySets.contains(new LinkedHashSet<>(constraint.getProperties()))) {
                issues.add(new Issue(
                        "redundant_node_constraint_key_overlap",
                        "Constraint '" + constraintId + "' (" + constraint.getType() + ") is redundant - "
                                + ("a KEY constraint already covers the same properties on node '" + nodeId + "'"),
                        "nodes." + nodeId + ".constraints." + constraintId));
            }
        }
    }
}
