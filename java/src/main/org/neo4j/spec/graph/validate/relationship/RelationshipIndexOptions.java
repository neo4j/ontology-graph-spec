// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipIndexOptions.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.model.relationship.extension.RelationshipIndex;
import org.neo4j.spec.graph.validate.Issue;

public final class RelationshipIndexOptions implements RelationshipValidation {
    public static final RelationshipIndexOptions INSTANCE = new RelationshipIndexOptions();

    private RelationshipIndexOptions() {
    }

    @Override
    public void validateIndex(GraphModel model, String relationshipId, Relationship relationship,
            String indexId, RelationshipIndex index, List<Issue> issues) {
        if (index.getOptions() != null && index.getOptions().getType() != index.getType()) {
            issues.add(new Issue("relationship_index_type_options_mismatch", "Cannot use options type '" + index.getOptions().getType() + "' with relationship index '" + indexId + "' " + ("type '" + index.getType() + "'"), "relationships." + relationshipId + ".extensions.indexes." + indexId + ".options"));
        }
    }
}
