// Generated from org/neo4j/spec/graph/validate/node/constraint/NodeExistenceCompositeConflict.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node.constraint;

import java.util.List;
import org.neo4j.spec.graph.internal.Kt;
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
        List<NodeConstraint> compositeConstraints = Kt.filter(node.getConstraints().values(), it -> it.getType() != ConstraintType.EXISTS && Compositeness.isCompositeConstraint(it.getProperties()));
        for (var entry : node.getConstraints().entrySet()) {
            var constraintId = entry.getKey();
            var constraint = entry.getValue();
            if (constraint.getType() != ConstraintType.EXISTS || Kt.distinct(constraint.getProperties()).size() != 1) {
                continue;
            }
            String propertyId = Kt.first(constraint.getProperties());
            boolean conflictsWithComposite = Kt.any(compositeConstraints, other -> other.getProperties().contains(propertyId));
            if (conflictsWithComposite) {
                issues.add(new Issue("node_existence_composite_conflict", "Existence constraint '" + constraintId + "' on property " + ("'" + propertyId + "' conflicts with a composite constraint on node '" + nodeId + "'"), "nodes." + nodeId + ".constraints." + constraintId));
            }
        }
    }
}
