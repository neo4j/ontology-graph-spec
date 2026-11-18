// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipIndexes.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.model.relationship.extension.RelationshipIndex;
import org.neo4j.spec.graph.validate.Issue;

public final class RelationshipIndexes implements RelationshipValidation {
    public static final RelationshipIndexes INSTANCE = new RelationshipIndexes();

    private RelationshipIndexes() {
    }

    @Override
    public void validateIndex(GraphModel model, String relationshipId, Relationship relationship,
            String indexId, RelationshipIndex index, List<Issue> issues) {
        for (var property : index.getProperties()) {
            if (!relationship.getProperties().containsKey(property)) {
                issues.add(new Issue("missing_relationship_index_property", "Missing property with id '" + property + "' for relationship index '" + indexId + "'", "relationships." + relationshipId + ".extensions.indexes." + indexId + ".properties." + property));
            }
        }
    }
}
