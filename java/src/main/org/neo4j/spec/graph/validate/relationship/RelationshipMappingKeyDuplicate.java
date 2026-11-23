/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    private RelationshipMappingKeyDuplicate() {}

    @Override
    public void validateRelationship(
            GraphModel model, String relationshipId, Relationship relationship, List<Issue> issues) {
        for (var entry : model.getExtensions().getDataSourceMappings().entrySet()) {
            var mappingId = entry.getKey();
            var mappings = entry.getValue();
            for (int index = 0; index < mappings.size(); index++) {
                var mapping = mappings.get(index);
                if (!(mapping instanceof RelationshipMapping)
                        || !Objects.equals(((RelationshipMapping) mapping).getRelationship(), relationshipId)) {
                    continue;
                }
                issues.addAll(DuplicateEntries.duplicateEntryIssues(
                        "duplicate_relationship_mapping_key",
                        "mapping key",
                        "relationship '" + relationshipId + "'",
                        ((RelationshipMapping) mapping).getKey(),
                        Issue.jsonPointerOf("extensions", "data_source_mappings", mappingId, index, "key")));
            }
        }
    }
}
