// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipMappingKey.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import java.util.Objects;
import org.neo4j.spec.graph.internal.Kt;
import org.neo4j.spec.graph.internal.Pair;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.extension.mapping.RelationshipMapping;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.validate.Issue;

/**
 * The key of a relationship mapping identifies the relationship so may only contain the relationship's own
 * properties.
 */
public final class RelationshipMappingKey implements RelationshipValidation {
    public static final RelationshipMappingKey INSTANCE = new RelationshipMappingKey();

    private RelationshipMappingKey() {
    }

    @Override
    public void validateRelationship(GraphModel model, String relationshipId,
            Relationship relationship, List<Issue> issues) {
        for (var entry : model.getExtensions().getDataSourceMappings().entrySet()) {
            var mappingId = entry.getKey();
            var mappings = entry.getValue();
            for (var item : Kt.withIndex(mappings)) {
                var index = item.getIndex();
                var mapping = item.getValue();
                if (!(mapping instanceof RelationshipMapping) || !Objects.equals(((RelationshipMapping) ((RelationshipMapping) mapping)).getRelationship(), relationshipId)) {
                    continue;
                }
                for (var propertyId : Kt.filterNot(((RelationshipMapping) ((RelationshipMapping) mapping)).getKey(), it -> relationship.getProperties().containsKey(it))) {
                    issues.add(new Issue("invalid_relationship_mapping_key", "Mapping key '" + propertyId + "' is not a property of relationship '" + relationshipId + "'", "extensions.data_source_mappings." + mappingId + "[" + index + "].key", Kt.mapOf(new Pair<>("relationship", relationshipId), new Pair<>("property", propertyId))));
                }
            }
        }
    }
}
