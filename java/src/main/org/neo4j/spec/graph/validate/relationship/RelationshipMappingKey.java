// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipMappingKey.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
            for (int index = 0; index < mappings.size(); index++) {
                var mapping = mappings.get(index);
                if (!(mapping instanceof RelationshipMapping) || !Objects.equals(((RelationshipMapping) ((RelationshipMapping) mapping)).getRelationship(), relationshipId)) {
                    continue;
                }
                for (var propertyId : ((RelationshipMapping) mapping).getKey()) {
                    if (relationship.getProperties().containsKey(propertyId)) {
                        continue;
                    }
                    Map<String, String> details = new LinkedHashMap<>();
                    details.put("relationship", relationshipId);
                    details.put("property", propertyId);
                    issues.add(new Issue("invalid_relationship_mapping_key", "Mapping key '" + propertyId + "' is not a property of relationship '" + relationshipId + "'", "extensions.data_source_mappings." + mappingId + "[" + index + "].key", details));
                }
            }
        }
    }
}
