// Generated from org/neo4j/spec/graph/validate/node/LocatedNodeMapping.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.extension.mapping.NodeMapping;

public final class LocatedNodeMappings {
    private LocatedNodeMappings() {
    }

    /**
     * Finds every mapping of {@code nodeId} along with where each lives in the model's mappings.
     */
    public static List<LocatedNodeMapping> findNodeMappings(GraphModel model, String nodeId) {
        List<LocatedNodeMapping> found = new ArrayList<LocatedNodeMapping>();
        for (var entry : model.getExtensions().getDataSourceMappings().entrySet()) {
            var mappingId = entry.getKey();
            var mappings = entry.getValue();
            for (int index = 0; index < mappings.size(); index++) {
                var mapping = mappings.get(index);
                if (mapping instanceof NodeMapping && Objects.equals(((NodeMapping) ((NodeMapping) mapping)).getNode(), nodeId)) {
                    found.add(new LocatedNodeMapping(mappingId, index, ((NodeMapping) mapping)));
                }
            }
        }
        return found;
    }
}
