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

import java.util.List;
import java.util.stream.Collectors;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.NodeConstraint;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.constraint.Duplication;
import org.neo4j.spec.graph.validate.node.NodeValidation;

public final class NodeConstraintDuplicatePropertySet implements NodeValidation {
    public static final NodeConstraintDuplicatePropertySet INSTANCE = new NodeConstraintDuplicatePropertySet();

    private NodeConstraintDuplicatePropertySet() {}

    @Override
    public void validateConstraint(
            GraphModel model,
            String nodeId,
            Node node,
            String constraintId,
            NodeConstraint constraint,
            List<Issue> issues) {
        List<String> duplicates = Duplication.duplicateConstraintIds(constraintId, node.getConstraints());
        if (!duplicates.isEmpty()) {
            String properties = String.join(", ", constraint.getProperties());
            issues.add(new Issue(
                    "duplicate_node_constraint_property_set",
                    "Constraint '" + constraintId + "' (" + constraint.getType() + " on " + properties + ") on node '"
                            + nodeId + "' "
                            + ("duplicates "
                                    + duplicates.stream()
                                            .map(it -> "'" + it + "'")
                                            .collect(Collectors.joining(", "))),
                    "nodes." + nodeId + ".constraints." + constraintId + ".properties"));
        }
    }
}
