// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipNodes.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.validate.Issue;

public final class RelationshipNodes implements RelationshipValidation {
    public static final RelationshipNodes INSTANCE = new RelationshipNodes();

    private RelationshipNodes() {
    }

    @Override
    public void validateRelationship(GraphModel model, String relationshipId,
            Relationship relationship, List<Issue> issues) {
        if (!relationship.getFrom().getNode().isBlank() && !model.getNodes().containsKey(relationship.getFrom().getNode())) {
            issues.add(new Issue("missing_relationship_from_node", "Missing node with id '" + relationship.getFrom().getNode() + "' for relationship '" + relationshipId + "'", "relationships." + relationshipId + ".from." + relationship.getFrom().getNode()));
        }
        if (!relationship.getTo().getNode().isBlank() && !model.getNodes().containsKey(relationship.getTo().getNode())) {
            issues.add(new Issue("missing_relationship_to_node", "Missing node with id '" + relationship.getTo().getNode() + "' for relationship '" + relationshipId + "'", "relationships." + relationshipId + ".to." + relationship.getTo().getNode()));
        }
    }
}
