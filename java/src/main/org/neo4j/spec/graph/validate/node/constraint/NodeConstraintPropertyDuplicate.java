// Generated from org/neo4j/spec/graph/validate/node/constraint/NodeConstraintPropertyDuplicate.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node.constraint;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.NodeConstraint;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntriesKt;
import org.neo4j.spec.graph.validate.node.NodeValidation;

/**
 * A node constraint must list each property once.
 */
public final class NodeConstraintPropertyDuplicate implements NodeValidation {
    public static final NodeConstraintPropertyDuplicate INSTANCE = new NodeConstraintPropertyDuplicate();

    private NodeConstraintPropertyDuplicate() {
    }

    @Override
    public void validateConstraint(GraphModel model, String nodeId, Node node, String constraintId,
            NodeConstraint constraint, List<Issue> issues) {
        issues.addAll(DuplicateEntriesKt.duplicateEntryIssues("duplicate_node_constraint_property", "property", "constraint '" + constraintId + "' on node '" + nodeId + "'", constraint.getProperties(), "nodes." + nodeId + ".constraints." + constraintId + ".properties"));
    }
}
