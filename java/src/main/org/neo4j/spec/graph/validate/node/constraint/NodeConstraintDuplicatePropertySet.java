// Generated from org/neo4j/spec/graph/validate/node/constraint/NodeConstraintDuplicatePropertySet.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node.constraint;

import java.util.List;
import org.neo4j.spec.graph.internal.Kt;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.NodeConstraint;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.constraint.DuplicationKt;
import org.neo4j.spec.graph.validate.node.NodeValidation;

public final class NodeConstraintDuplicatePropertySet implements NodeValidation {
    public static final NodeConstraintDuplicatePropertySet INSTANCE = new NodeConstraintDuplicatePropertySet();

    private NodeConstraintDuplicatePropertySet() {
    }

    @Override
    public void validateConstraint(GraphModel model, String nodeId, Node node, String constraintId,
            NodeConstraint constraint, List<Issue> issues) {
        List<String> duplicates = DuplicationKt.duplicateConstraintIds(constraintId, node.getConstraints());
        if (!duplicates.isEmpty()) {
            String properties = Kt.joinToString(constraint.getProperties());
            issues.add(new Issue("duplicate_node_constraint_property_set", "Constraint '" + constraintId + "' (" + constraint.getType() + " on " + properties + ") on node '" + nodeId + "' " + ("duplicates " + Kt.joinToString(duplicates, it -> "'" + it + "'")), "nodes." + nodeId + ".constraints." + constraintId + ".properties"));
        }
    }
}
