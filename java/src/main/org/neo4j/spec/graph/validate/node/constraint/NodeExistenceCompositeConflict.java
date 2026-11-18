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
package org.neo4j.spec.graph.validate.node.constraint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.NodeConstraint;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.constraint.Compositeness;
import org.neo4j.spec.graph.validate.node.NodeValidation;

public final class NodeExistenceCompositeConflict implements NodeValidation {
    public static final NodeExistenceCompositeConflict INSTANCE = new NodeExistenceCompositeConflict();

    private NodeExistenceCompositeConflict() {}

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        List<NodeConstraint> compositeConstraints = new ArrayList<>();
        for (var other : node.getConstraints().values()) {
            if (other.getType() != ConstraintType.EXISTS
                    && Compositeness.isCompositeConstraint(other.getProperties())) {
                compositeConstraints.add(other);
            }
        }
        for (var entry : node.getConstraints().entrySet()) {
            var constraintId = entry.getKey();
            var constraint = entry.getValue();
            if (constraint.getType() != ConstraintType.EXISTS
                    || new HashSet<>(constraint.getProperties()).size() != 1) {
                continue;
            }
            String propertyId = constraint.getProperties().get(0);
            boolean conflictsWithComposite = false;
            for (var other : compositeConstraints) {
                if (other.getProperties().contains(propertyId)) {
                    conflictsWithComposite = true;
                    break;
                }
            }
            if (conflictsWithComposite) {
                issues.add(new Issue(
                        "node_existence_composite_conflict",
                        "Existence constraint '" + constraintId + "' on property "
                                + ("'" + propertyId + "' conflicts with a composite constraint on node '" + nodeId
                                        + "'"),
                        "nodes." + nodeId + ".constraints." + constraintId));
            }
        }
    }
}
