// Generated from org/neo4j/spec/graph/validate/node/NodeIndexLabelDuplicate.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.node.extension.NodeIndex;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntriesKt;

/**
 * A node index must list each label once.
 */
public final class NodeIndexLabelDuplicate implements NodeValidation {
    public static final NodeIndexLabelDuplicate INSTANCE = new NodeIndexLabelDuplicate();

    private NodeIndexLabelDuplicate() {
    }

    @Override
    public void validateIndex(GraphModel model, String nodeId, Node node, String indexId,
            NodeIndex index, List<Issue> issues) {
        issues.addAll(DuplicateEntriesKt.duplicateEntryIssues("duplicate_node_index_label", "label", "index '" + indexId + "' on node '" + nodeId + "'", index.getLabels(), "nodes." + nodeId + ".extensions.indexes." + indexId + ".labels"));
    }
}
