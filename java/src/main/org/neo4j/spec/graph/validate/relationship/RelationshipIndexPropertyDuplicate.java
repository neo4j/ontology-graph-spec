// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipIndexPropertyDuplicate.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.model.relationship.extension.RelationshipIndex;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntries;

/**
 * A relationship index must list each property once.
 */
public final class RelationshipIndexPropertyDuplicate implements RelationshipValidation {
    public static final RelationshipIndexPropertyDuplicate INSTANCE = new RelationshipIndexPropertyDuplicate();

    private RelationshipIndexPropertyDuplicate() {
    }

    @Override
    public void validateIndex(GraphModel model, String relationshipId, Relationship relationship,
            String indexId, RelationshipIndex index, List<Issue> issues) {
        issues.addAll(DuplicateEntries.duplicateEntryIssues("duplicate_relationship_index_property", "property", "index '" + indexId + "' on relationship '" + relationshipId + "'", index.getProperties(), "relationships." + relationshipId + ".extensions.indexes." + indexId + ".properties"));
    }
}
