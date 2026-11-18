// Generated from org/neo4j/spec/graph/validate/relationship/constraint/RelationshipConstraintPropertyDuplicate.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship.constraint;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.model.relationship.RelationshipConstraint;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntries;
import org.neo4j.spec.graph.validate.relationship.RelationshipValidation;

/**
 * A relationship constraint must list each property once.
 */
public final class RelationshipConstraintPropertyDuplicate implements RelationshipValidation {
    public static final RelationshipConstraintPropertyDuplicate INSTANCE = new RelationshipConstraintPropertyDuplicate();

    private RelationshipConstraintPropertyDuplicate() {
    }

    @Override
    public void validateConstraint(GraphModel model, String relationshipId,
            Relationship relationship, String constraintId, RelationshipConstraint constraint,
            List<Issue> issues) {
        issues.addAll(DuplicateEntries.duplicateEntryIssues("duplicate_relationship_constraint_property", "property", "constraint '" + constraintId + "' on relationship '" + relationshipId + "'", constraint.getProperties(), "relationships." + relationshipId + ".constraints." + constraintId + ".properties"));
    }
}
