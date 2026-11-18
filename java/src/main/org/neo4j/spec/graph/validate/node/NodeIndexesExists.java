// Generated from org/neo4j/spec/graph/validate/node/NodeIndexesExists.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.extension.NodeIndex;
import org.neo4j.spec.graph.validate.Issue;

public final class NodeIndexesExists implements NodeValidation {
    public static final NodeIndexesExists INSTANCE = new NodeIndexesExists();

    private NodeIndexesExists() {
    }

    @Override
    public void validateIndex(GraphModel model, String nodeId, Node node, String indexId,
            NodeIndex index, List<Issue> issues) {
        for (var property : index.getProperties()) {
            if (!node.getProperties().containsKey(property)) {
                issues.add(new Issue("missing_node_index_property", "Missing property with id '" + property + "' for node index '" + indexId + "'", "nodes." + nodeId + ".extensions.indexes." + indexId + ".properties." + property));
            }
        }
    }
}
