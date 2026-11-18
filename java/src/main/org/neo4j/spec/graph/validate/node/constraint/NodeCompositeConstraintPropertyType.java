// Generated from org/neo4j/spec/graph/validate/node/constraint/NodeCompositeConstraintPropertyType.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node.constraint;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.NodeConstraint;
import org.neo4j.spec.graph.model.property.Neo4jType;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.constraint.Compositeness;
import org.neo4j.spec.graph.validate.node.NodeValidation;

public final class NodeCompositeConstraintPropertyType implements NodeValidation {
    public static final NodeCompositeConstraintPropertyType INSTANCE = new NodeCompositeConstraintPropertyType();

    private NodeCompositeConstraintPropertyType() {
    }

    @Override
    public void validateConstraint(GraphModel model, String nodeId, Node node, String constraintId,
            NodeConstraint constraint, List<Issue> issues) {
        if (!Compositeness.isCompositeConstraint(constraint.getProperties())) {
            return;
        }
        boolean hasInvalidType = false;
        for (var propertyId : constraint.getProperties()) {
            Property property = node.getProperties().get(propertyId);
            if (property != null && property.getType() != Neo4jType.STRING && property.getType() != Neo4jType.INTEGER) {
                hasInvalidType = true;
                break;
            }
        }
        if (hasInvalidType) {
            issues.add(new Issue("invalid_node_composite_constraint_property_type", "Composite constraint '" + constraintId + "' on node '" + nodeId + "' " + "properties must be STRING or INTEGER for bulk import", "nodes." + nodeId + ".constraints." + constraintId));
        }
    }
}
