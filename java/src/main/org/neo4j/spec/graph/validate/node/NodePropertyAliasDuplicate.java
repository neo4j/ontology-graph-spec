// Generated from org/neo4j/spec/graph/validate/node/NodePropertyAliasDuplicate.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.node;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.node.Node;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntries;

/**
 * A node property's aliases are alternative names so must not repeat.
 */
public final class NodePropertyAliasDuplicate implements NodeValidation {
    public static final NodePropertyAliasDuplicate INSTANCE = new NodePropertyAliasDuplicate();

    private NodePropertyAliasDuplicate() {
    }

    @Override
    public void validateProperty(GraphModel model, String nodeId, Node node, String propertyId,
            Property property, List<Issue> issues) {
        issues.addAll(DuplicateEntries.duplicateEntryIssues("duplicate_node_property_alias", "alias", "property '" + propertyId + "' of node '" + nodeId + "'", property.getAliases(), "nodes." + nodeId + ".properties." + propertyId + ".aliases"));
    }
}
