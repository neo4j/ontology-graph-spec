// Generated from org/neo4j/spec/graph/validate/node/NodeIndexPropertyDuplicate.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.extension.NodeIndex;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntries;

/**
 * A node index must list each property once.
 */
public final class NodeIndexPropertyDuplicate implements NodeValidation {
    public static final NodeIndexPropertyDuplicate INSTANCE = new NodeIndexPropertyDuplicate();

    private NodeIndexPropertyDuplicate() {
    }

    @Override
    public void validateIndex(GraphModel model, String nodeId, Node node, String indexId,
            NodeIndex index, List<Issue> issues) {
        issues.addAll(DuplicateEntries.duplicateEntryIssues("duplicate_node_index_property", "property", "index '" + indexId + "' on node '" + nodeId + "'", index.getProperties(), "nodes." + nodeId + ".extensions.indexes." + indexId + ".properties"));
    }
}
