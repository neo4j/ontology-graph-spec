// Generated from org/neo4j/spec/graph/validate/node/NodePropertyEmptyName.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.property.PropertyNames;

public final class NodePropertyEmptyName implements NodeValidation {
    public static final NodePropertyEmptyName INSTANCE = new NodePropertyEmptyName();

    private NodePropertyEmptyName() {
    }

    @Override
    public void validateProperty(GraphModel model, String nodeId, Node node, String propertyId,
            Property property, List<Issue> issues) {
        Issue receiver = PropertyNames.emptyPropertyName(propertyId, property, "nodes." + nodeId);
        if (receiver != null) {
            issues.add(receiver);
        }
    }
}
