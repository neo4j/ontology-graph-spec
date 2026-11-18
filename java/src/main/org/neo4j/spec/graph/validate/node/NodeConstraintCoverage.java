// Generated from org/neo4j/spec/graph/validate/node/NodeConstraintCoverage.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.validate.Issue;

public final class NodeConstraintCoverage implements NodeValidation {
    public static final NodeConstraintCoverage INSTANCE = new NodeConstraintCoverage();

    private NodeConstraintCoverage() {
    }

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        if (node.getConstraints().isEmpty()) {
            issues.add(new Issue("missing_node_constraint_coverage", "Node '" + nodeId + "' has no constraints — add a KEY, UNIQUENESS, or EXISTENCE constraint.", "nodes." + nodeId + ".constraints"));
        }
    }
}
