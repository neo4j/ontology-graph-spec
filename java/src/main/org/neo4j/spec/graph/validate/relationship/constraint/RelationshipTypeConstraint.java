// Generated from org/neo4j/spec/graph/validate/relationship/constraint/RelationshipTypeConstraint.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship.constraint;

import java.util.List;
import org.neo4j.spec.graph.internal.Kt;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.model.relationship.RelationshipConstraint;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validation;
import org.neo4j.spec.graph.validate.node.NodeConstraints;
import org.neo4j.spec.graph.validate.relationship.RelationshipValidation;

public final class RelationshipTypeConstraint implements RelationshipValidation {
    public static final RelationshipTypeConstraint INSTANCE = new RelationshipTypeConstraint();

    private RelationshipTypeConstraint() {
    }

    @Override
    public List<Validation> dependsOn() {
        return Kt.listOf(NodeConstraints.INSTANCE);
    }

    @Override
    public void validateConstraint(GraphModel model, String relationshipId,
            Relationship relationship, String constraintId, RelationshipConstraint constraint,
            List<Issue> issues) {
        if (constraint.getType() == ConstraintType.PROPERTY_TYPE && Kt.distinct(constraint.getProperties()).size() != 1) {
            issues.add(new Issue("invalid_relationship_type_constraint_property_count", "Relationship type constraint '" + constraintId + "' must have exactly one property.", "relationships." + relationshipId + ".constraints." + constraintId + ".properties"));
        }
    }
}
