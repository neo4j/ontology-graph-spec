// Generated from org/neo4j/spec/graph/validate/node/NodeMappingKeyDuplicate.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntries;

/**
 * A node mapping key must list each property once.
 */
public final class NodeMappingKeyDuplicate implements NodeValidation {
    public static final NodeMappingKeyDuplicate INSTANCE = new NodeMappingKeyDuplicate();

    private NodeMappingKeyDuplicate() {
    }

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        for (var item : LocatedNodeMappings.findNodeMappings(model, nodeId)) {
            var mappingId = item.getMappingId();
            var index = item.getIndex();
            var nodeMapping = item.getMapping();
            issues.addAll(DuplicateEntries.duplicateEntryIssues("duplicate_node_mapping_key", "mapping key", "node '" + nodeId + "'", nodeMapping.getKey(), "extensions.data_source_mappings." + mappingId + "[" + index + "].key"));
        }
    }
}
