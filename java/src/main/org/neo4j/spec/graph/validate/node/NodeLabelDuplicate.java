// Generated from org/neo4j/spec/graph/validate/node/NodeLabelDuplicate.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntries;

/**
 * A node's implied labels and optional labels must each list a label once.
 */
public final class NodeLabelDuplicate implements NodeValidation {
    public static final NodeLabelDuplicate INSTANCE = new NodeLabelDuplicate();

    private NodeLabelDuplicate() {
    }

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        issues.addAll(DuplicateEntries.duplicateEntryIssues("duplicate_node_implied_label", "implied label", "node '" + nodeId + "'", node.getLabels().getImplied(), "nodes." + nodeId + ".labels.implied"));
        issues.addAll(DuplicateEntries.duplicateEntryIssues("duplicate_node_optional_label", "optional label", "node '" + nodeId + "'", node.getLabels().getOptional(), "nodes." + nodeId + ".labels.optional"));
    }
}
