// Generated from org/neo4j/spec/graph/validate/node/NodeProperties.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.validate.Issue;

public final class NodeProperties implements NodeValidation {
    public static final NodeProperties INSTANCE = new NodeProperties();

    private NodeProperties() {
    }

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        if (node.getProperties().isEmpty()) {
            issues.add(new Issue("missing_node_properties", "Node '" + nodeId + "' has no properties", "nodes." + nodeId + ".properties"));
        }
    }
}
