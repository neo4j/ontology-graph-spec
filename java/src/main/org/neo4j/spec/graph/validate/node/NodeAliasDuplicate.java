// Generated from org/neo4j/spec/graph/validate/node/NodeAliasDuplicate.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntries;

/**
 * A node's aliases are alternative names so must not repeat.
 */
public final class NodeAliasDuplicate implements NodeValidation {
    public static final NodeAliasDuplicate INSTANCE = new NodeAliasDuplicate();

    private NodeAliasDuplicate() {
    }

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        issues.addAll(DuplicateEntries.duplicateEntryIssues("duplicate_node_alias", "alias", "node '" + nodeId + "'", node.getAliases(), "nodes." + nodeId + ".aliases"));
    }
}
