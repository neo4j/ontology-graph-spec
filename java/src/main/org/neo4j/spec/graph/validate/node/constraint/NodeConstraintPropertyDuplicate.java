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
// Generated from org/neo4j/spec/graph/validate/node/constraint/NodeConstraintPropertyDuplicate.kt by :java:generator.
// Do not edit.
package org.neo4j.spec.graph.validate.node.constraint;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.NodeConstraint;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntries;
import org.neo4j.spec.graph.validate.node.NodeValidation;

/**
 * A node constraint must list each property once.
 */
public final class NodeConstraintPropertyDuplicate implements NodeValidation {
    public static final NodeConstraintPropertyDuplicate INSTANCE = new NodeConstraintPropertyDuplicate();

    private NodeConstraintPropertyDuplicate() {}

    @Override
    public void validateConstraint(
            GraphModel model,
            String nodeId,
            Node node,
            String constraintId,
            NodeConstraint constraint,
            List<Issue> issues) {
        issues.addAll(DuplicateEntries.duplicateEntryIssues(
                "duplicate_node_constraint_property",
                "property",
                "constraint '" + constraintId + "' on node '" + nodeId + "'",
                constraint.getProperties(),
                "nodes." + nodeId + ".constraints." + constraintId + ".properties"));
    }
}
