// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipPropertyAliasDuplicate.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntriesKt;

/**
 * A relationship property's aliases are alternative names so must not repeat.
 */
public final class RelationshipPropertyAliasDuplicate implements RelationshipValidation {
    public static final RelationshipPropertyAliasDuplicate INSTANCE = new RelationshipPropertyAliasDuplicate();

    private RelationshipPropertyAliasDuplicate() {
    }

    @Override
    public void validateProperty(GraphModel model, String relationshipId, Relationship relationship,
            String propertyId, Property property, List<Issue> issues) {
        issues.addAll(DuplicateEntriesKt.duplicateEntryIssues("duplicate_relationship_property_alias", "alias", "property '" + propertyId + "' of relationship '" + relationshipId + "'", property.getAliases(), "relationships." + relationshipId + ".properties." + propertyId + ".aliases"));
    }
}
