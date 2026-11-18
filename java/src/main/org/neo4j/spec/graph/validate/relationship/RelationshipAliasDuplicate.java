// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipAliasDuplicate.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntriesKt;

/**
 * A relationship's aliases are alternative names so must not repeat.
 */
public final class RelationshipAliasDuplicate implements RelationshipValidation {
    public static final RelationshipAliasDuplicate INSTANCE = new RelationshipAliasDuplicate();

    private RelationshipAliasDuplicate() {
    }

    @Override
    public void validateRelationship(GraphModel model, String relationshipId,
            Relationship relationship, List<Issue> issues) {
        issues.addAll(DuplicateEntriesKt.duplicateEntryIssues("duplicate_relationship_alias", "alias", "relationship '" + relationshipId + "'", relationship.getAliases(), "relationships." + relationshipId + ".aliases"));
    }
}
