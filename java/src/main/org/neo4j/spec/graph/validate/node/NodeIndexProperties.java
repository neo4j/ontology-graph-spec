// Generated from org/neo4j/spec/graph/validate/node/NodeIndexProperties.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.extension.NodeIndex;
import org.neo4j.spec.graph.validate.Issue;

public final class NodeIndexProperties implements NodeValidation {
    public static final NodeIndexProperties INSTANCE = new NodeIndexProperties();

    private NodeIndexProperties() {
    }

    @Override
    public void validateIndex(GraphModel model, String nodeId, Node node, String indexId,
            NodeIndex index, List<Issue> issues) {
        if (index.getProperties().isEmpty()) {
            issues.add(new Issue("missing_node_index_properties", "Index '" + indexId + "' on node '" + nodeId + "' must have at least one property", "nodes." + nodeId + ".extensions.indexes." + indexId + ".properties"));
        }
    }
}
