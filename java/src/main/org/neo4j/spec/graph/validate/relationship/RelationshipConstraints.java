// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipConstraints.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.model.relationship.RelationshipConstraint;
import org.neo4j.spec.graph.validate.Issue;

public final class RelationshipConstraints implements RelationshipValidation {
    public static final RelationshipConstraints INSTANCE = new RelationshipConstraints();

    private RelationshipConstraints() {
    }

    @Override
    public void validateConstraint(GraphModel model, String relationshipId,
            Relationship relationship, String constraintId, RelationshipConstraint constraint,
            List<Issue> issues) {
        for (var property : constraint.getProperties()) {
            if (!relationship.getProperties().containsKey(property)) {
                issues.add(new Issue("missing_relationship_constraint_property", "Missing property with id '" + property + "' for relationship constraint '" + constraintId + "'", "relationships." + relationshipId + ".constraints." + constraintId + ".properties." + property));
            }
        }
    }
}
