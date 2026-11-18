// Generated from org/neo4j/spec/graph/validate/node/NodeConstraints.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.NodeConstraint;
import org.neo4j.spec.graph.validate.Issue;

public final class NodeConstraints implements NodeValidation {
    public static final NodeConstraints INSTANCE = new NodeConstraints();

    private NodeConstraints() {
    }

    @Override
    public void validateConstraint(GraphModel model, String nodeId, Node node, String constraintId,
            NodeConstraint constraint, List<Issue> issues) {
        for (var property : constraint.getProperties()) {
            if (!node.getProperties().containsKey(property)) {
                issues.add(new Issue("missing_node_constraint_property", "Missing property with id '" + property + "' for node constraint '" + constraintId + "'", "nodes." + nodeId + ".constraints." + constraintId + ".properties." + property));
            }
        }
    }
}
