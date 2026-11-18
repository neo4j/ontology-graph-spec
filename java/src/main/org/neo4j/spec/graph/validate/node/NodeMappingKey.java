// Generated from org/neo4j/spec/graph/validate/node/NodeMappingKey.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.validate.Issue;

public final class NodeMappingKey implements NodeValidation {
    public static final NodeMappingKey INSTANCE = new NodeMappingKey();

    private NodeMappingKey() {
    }

    @Override
    public void validateNode(GraphModel model, String nodeId, Node node, List<Issue> issues) {
        for (var item : LocatedNodeMappings.findNodeMappings(model, nodeId)) {
            var mappingId = item.getMappingId();
            var index = item.getIndex();
            var nodeMapping = item.getMapping();
            if (nodeMapping.getKey().isEmpty()) {
                issues.add(new Issue("missing_node_mapping_key", "Node '" + nodeId + "' has no mapping key defined", "extensions.data_source_mappings." + mappingId + "[" + index + "].key"));
            }
        }
    }
}
