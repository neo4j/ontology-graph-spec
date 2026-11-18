// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipType.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.validate.Issue;

public final class RelationshipType implements RelationshipValidation {
    public static final RelationshipType INSTANCE = new RelationshipType();

    private RelationshipType() {
    }

    @Override
    public void validateRelationship(GraphModel model, String relationshipId,
            Relationship relationship, List<Issue> issues) {
        if (relationship.getType().isBlank()) {
            issues.add(new Issue("missing_relationship_type", "Missing type for relationship '" + relationshipId + "'", "relationships." + relationshipId + ".type"));
        }
    }
}
