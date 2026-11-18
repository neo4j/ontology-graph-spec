// Generated from org/neo4j/spec/graph/validate/node/NodeLabel.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.internal.Kt;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.validate.Issue;

public final class NodeLabel implements NodeValidation {
    public static final NodeLabel INSTANCE = new NodeLabel();

    private NodeLabel() {
    }

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        if (Kt.isNullOrBlank(node.getLabels().getIdentifier())) {
            issues.add(new Issue("missing_node_identifier_label", "Missing identifier label for node '" + nodeId + "'", "nodes." + nodeId + ".labels.identifier"));
        }
    }
}
