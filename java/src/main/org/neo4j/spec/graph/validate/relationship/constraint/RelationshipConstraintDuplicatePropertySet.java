// Generated from org/neo4j/spec/graph/validate/relationship/constraint/RelationshipConstraintDuplicatePropertySet.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship.constraint;

import java.util.List;
import java.util.stream.Collectors;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.model.relationship.RelationshipConstraint;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.constraint.Duplication;
import org.neo4j.spec.graph.validate.relationship.RelationshipValidation;

public final class RelationshipConstraintDuplicatePropertySet implements RelationshipValidation {
    public static final RelationshipConstraintDuplicatePropertySet INSTANCE = new RelationshipConstraintDuplicatePropertySet();

    private RelationshipConstraintDuplicatePropertySet() {
    }

    @Override
    public void validateConstraint(GraphModel model, String relationshipId,
            Relationship relationship, String constraintId, RelationshipConstraint constraint,
            List<Issue> issues) {
        List<String> duplicates = Duplication.duplicateConstraintIds(constraintId, relationship.getConstraints());
        if (!duplicates.isEmpty()) {
            String properties = String.join(", ", constraint.getProperties());
            issues.add(new Issue("duplicate_relationship_constraint_property_set", "Constraint '" + constraintId + "' (" + constraint.getType() + " on " + properties + ") " + ("on relationship '" + relationshipId + "' duplicates " + duplicates.stream().map(it -> "'" + it + "'").collect(Collectors.joining(", "))), "relationships." + relationshipId + ".constraints." + constraintId + ".properties"));
        }
    }
}
