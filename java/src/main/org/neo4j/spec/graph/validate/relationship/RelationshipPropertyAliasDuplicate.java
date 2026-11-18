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
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.duplicate.DuplicateEntries;

/**
 * A relationship property's aliases are alternative names so must not repeat.
 */
public final class RelationshipPropertyAliasDuplicate implements RelationshipValidation {
    public static final RelationshipPropertyAliasDuplicate INSTANCE = new RelationshipPropertyAliasDuplicate();

    private RelationshipPropertyAliasDuplicate() {}

    @Override
    public void validateProperty(
            GraphModel model,
            String relationshipId,
            Relationship relationship,
            String propertyId,
            Property property,
            List<Issue> issues) {
        issues.addAll(DuplicateEntries.duplicateEntryIssues(
                "duplicate_relationship_property_alias",
                "alias",
                "property '" + propertyId + "' of relationship '" + relationshipId + "'",
                property.getAliases(),
                "relationships." + relationshipId + ".properties." + propertyId + ".aliases"));
    }
}
