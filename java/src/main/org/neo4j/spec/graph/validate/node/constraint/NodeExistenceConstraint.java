// Generated from org/neo4j/spec/graph/validate/node/constraint/NodeExistenceConstraint.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node.constraint;

import java.util.List;
import org.neo4j.spec.graph.internal.Kt;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.NodeConstraint;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validation;
import org.neo4j.spec.graph.validate.node.NodeConstraints;
import org.neo4j.spec.graph.validate.node.NodeValidation;

public final class NodeExistenceConstraint implements NodeValidation {
    public static final NodeExistenceConstraint INSTANCE = new NodeExistenceConstraint();

    private NodeExistenceConstraint() {
    }

    @Override
    public List<Validation> dependsOn() {
        return Kt.listOf(NodeConstraints.INSTANCE);
    }

    @Override
    public void validateConstraint(GraphModel model, String nodeId, Node node, String constraintId,
            NodeConstraint constraint, List<Issue> issues) {
        if (constraint.getType() == ConstraintType.EXISTS && Kt.distinct(constraint.getProperties()).size() != 1) {
            issues.add(new Issue("invalid_node_exist_constraint_property_count", "Node existence constraint '" + constraintId + "' must have exactly one property.", "nodes." + nodeId + ".constraints." + constraintId + ".properties"));
        }
    }
}
