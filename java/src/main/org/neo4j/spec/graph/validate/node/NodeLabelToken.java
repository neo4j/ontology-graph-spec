// Generated from org/neo4j/spec/graph/validate/node/NodeLabelToken.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.validate.Issue;

public final class NodeLabelToken implements NodeValidation {
    public static final NodeLabelToken INSTANCE = new NodeLabelToken();

    private NodeLabelToken() {
    }

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        String token = node.getLabels().getIdentifier();
        if (token != null && (token.contains(":") || token.contains("="))) {
            issues.add(new Issue("invalid_node_label_token", "Node label '" + token + "' contains invalid characters (: or =) for bulk import", "nodes." + nodeId + ".labels.identifier"));
        }
    }
}
