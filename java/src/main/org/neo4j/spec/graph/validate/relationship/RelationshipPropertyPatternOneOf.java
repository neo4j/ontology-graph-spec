// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipPropertyPatternOneOf.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.property.PropertyNames;

public final class RelationshipPropertyPatternOneOf implements RelationshipValidation {
    public static final RelationshipPropertyPatternOneOf INSTANCE = new RelationshipPropertyPatternOneOf();

    private RelationshipPropertyPatternOneOf() {
    }

    @Override
    public void validateProperty(GraphModel model, String relationshipId, Relationship relationship,
            String propertyId, Property property, List<Issue> issues) {
        Issue receiver = PropertyNames.patternOneOfConflict(propertyId, property, "relationships." + relationshipId);
        if (receiver != null) {
            issues.add(receiver);
        }
    }
}
