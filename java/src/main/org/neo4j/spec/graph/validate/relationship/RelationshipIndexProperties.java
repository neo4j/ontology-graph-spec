// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipIndexProperties.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.model.relationship.extension.RelationshipIndex;
import org.neo4j.spec.graph.validate.Issue;

public final class RelationshipIndexProperties implements RelationshipValidation {
    public static final RelationshipIndexProperties INSTANCE = new RelationshipIndexProperties();

    private RelationshipIndexProperties() {
    }

    @Override
    public void validateIndex(GraphModel model, String relationshipId, Relationship relationship,
            String indexId, RelationshipIndex index, List<Issue> issues) {
        if (index.getProperties().isEmpty()) {
            issues.add(new Issue("missing_relationship_index_properties", "Index '" + indexId + "' on relationship '" + relationshipId + "' must have at least one property", "relationships." + relationshipId + ".extensions.indexes." + indexId + ".properties"));
        }
    }
}
