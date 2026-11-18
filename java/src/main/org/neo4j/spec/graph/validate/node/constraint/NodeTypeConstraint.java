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
// Generated from org/neo4j/spec/graph/validate/node/constraint/NodeTypeConstraint.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node.constraint;

import java.util.HashSet;
import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.NodeConstraint;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validation;
import org.neo4j.spec.graph.validate.node.NodeConstraints;
import org.neo4j.spec.graph.validate.node.NodeValidation;

public final class NodeTypeConstraint implements NodeValidation {
    public static final NodeTypeConstraint INSTANCE = new NodeTypeConstraint();

    private NodeTypeConstraint() {}

    @Override
    public List<Validation> dependsOn() {
        return List.of(NodeConstraints.INSTANCE);
    }

    @Override
    public void validateConstraint(
            GraphModel model,
            String nodeId,
            Node node,
            String constraintId,
            NodeConstraint constraint,
            List<Issue> issues) {
        if (constraint.getType() == ConstraintType.PROPERTY_TYPE
                && new HashSet<>(constraint.getProperties()).size() != 1) {
            issues.add(new Issue(
                    "invalid_node_type_constraint_property_count",
                    "Node type constraint '" + constraintId + "' must have exactly one property.",
                    "nodes." + nodeId + ".constraints." + constraintId + ".properties"));
        }
    }
}
