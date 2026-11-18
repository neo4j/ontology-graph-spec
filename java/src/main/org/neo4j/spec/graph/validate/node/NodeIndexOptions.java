// Generated from org/neo4j/spec/graph/validate/node/NodeIndexOptions.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.extension.NodeIndex;
import org.neo4j.spec.graph.validate.Issue;

public final class NodeIndexOptions implements NodeValidation {
    public static final NodeIndexOptions INSTANCE = new NodeIndexOptions();

    private NodeIndexOptions() {
    }

    @Override
    public void validateIndex(GraphModel model, String nodeId, Node node, String indexId,
            NodeIndex index, List<Issue> issues) {
        if (index.getOptions() != null && index.getOptions().getType() != index.getType()) {
            issues.add(new Issue("node_index_type_options_mismatch", "Cannot use options type '" + index.getOptions().getType() + "' with node index '" + indexId + "' " + ("type '" + index.getType() + "'"), "nodes." + nodeId + ".extensions.indexes." + indexId + ".options"));
        }
    }
}
