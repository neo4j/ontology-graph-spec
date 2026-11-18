// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipMappingKeyDuplicate.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import java.util.Objects;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.extension.mapping.RelationshipMapping;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntries;

/**
 * A relationship mapping key must list each property once.
 */
public final class RelationshipMappingKeyDuplicate implements RelationshipValidation {
    public static final RelationshipMappingKeyDuplicate INSTANCE = new RelationshipMappingKeyDuplicate();

    private RelationshipMappingKeyDuplicate() {
    }

    @Override
    public void validateRelationship(GraphModel model, String relationshipId,
            Relationship relationship, List<Issue> issues) {
        for (var entry : model.getExtensions().getDataSourceMappings().entrySet()) {
            var mappingId = entry.getKey();
            var mappings = entry.getValue();
            for (int index = 0; index < mappings.size(); index++) {
                var mapping = mappings.get(index);
                if (!(mapping instanceof RelationshipMapping) || !Objects.equals(((RelationshipMapping) ((RelationshipMapping) mapping)).getRelationship(), relationshipId)) {
                    continue;
                }
                issues.addAll(DuplicateEntries.duplicateEntryIssues("duplicate_relationship_mapping_key", "mapping key", "relationship '" + relationshipId + "'", ((RelationshipMapping) ((RelationshipMapping) mapping)).getKey(), "extensions.data_source_mappings." + mappingId + "[" + index + "].key"));
            }
        }
    }
}
