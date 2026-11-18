// Generated from org/neo4j/spec/graph/validate/node/constraint/NodeConstraintProperties.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node.constraint;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.NodeConstraint;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.node.NodeValidation;

public final class NodeConstraintProperties implements NodeValidation {
    public static final NodeConstraintProperties INSTANCE = new NodeConstraintProperties();

    private NodeConstraintProperties() {
    }

    @Override
    public void validateConstraint(GraphModel model, String nodeId, Node node, String constraintId,
            NodeConstraint constraint, List<Issue> issues) {
        if (constraint.getProperties().isEmpty()) {
            issues.add(new Issue("missing_node_constraint_properties", "Constraint '" + constraintId + "' on node '" + nodeId + "' has no properties", "nodes." + nodeId + ".constraints." + constraintId + ".properties"));
        }
    }
}
