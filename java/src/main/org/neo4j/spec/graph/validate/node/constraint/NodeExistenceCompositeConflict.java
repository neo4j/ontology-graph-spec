// Generated from org/neo4j/spec/graph/validate/node/constraint/NodeExistenceCompositeConflict.kt by :java:generator. Do not edit.
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

    private NodeExistenceCompositeConflict() {
    }

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        List<NodeConstraint> compositeConstraints = new ArrayList<>();
        for (var other : node.getConstraints().values()) {
            if (other.getType() != ConstraintType.EXISTS && Compositeness.isCompositeConstraint(other.getProperties())) {
                compositeConstraints.add(other);
            }
        }
        for (var entry : node.getConstraints().entrySet()) {
            var constraintId = entry.getKey();
            var constraint = entry.getValue();
            if (constraint.getType() != ConstraintType.EXISTS || new HashSet<>(constraint.getProperties()).size() != 1) {
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
                issues.add(new Issue("node_existence_composite_conflict", "Existence constraint '" + constraintId + "' on property " + ("'" + propertyId + "' conflicts with a composite constraint on node '" + nodeId + "'"), "nodes." + nodeId + ".constraints." + constraintId));
            }
        }
    }
}
