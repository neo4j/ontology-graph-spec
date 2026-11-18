// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipTypeToken.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.validate.Issue;

public final class RelationshipTypeToken implements RelationshipValidation {
    public static final RelationshipTypeToken INSTANCE = new RelationshipTypeToken();

    private RelationshipTypeToken() {
    }

    @Override
    public void validateRelationship(GraphModel model, String relationshipId,
            Relationship relationship, List<Issue> issues) {
        String type = relationship.getType();
        if (type.contains(":") || type.contains("=")) {
            issues.add(new Issue("invalid_relationship_type_token", "Relationship type '" + type + "' contains invalid characters (: or =) for bulk import", "relationships." + relationshipId + ".type"));
        }
    }
}
