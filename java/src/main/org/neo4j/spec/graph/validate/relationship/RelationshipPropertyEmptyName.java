// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipPropertyEmptyName.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.property.PropertyNames;

public final class RelationshipPropertyEmptyName implements RelationshipValidation {
    public static final RelationshipPropertyEmptyName INSTANCE = new RelationshipPropertyEmptyName();

    private RelationshipPropertyEmptyName() {
    }

    @Override
    public void validateProperty(GraphModel model, String relationshipId, Relationship relationship,
            String propertyId, Property property, List<Issue> issues) {
        Issue receiver = PropertyNames.emptyPropertyName(propertyId, property, "relationships." + relationshipId);
        if (receiver != null) {
            issues.add(receiver);
        }
    }
}
