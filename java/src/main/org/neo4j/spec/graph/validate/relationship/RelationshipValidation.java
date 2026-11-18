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
// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipValidation.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.property.Property;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.model.relationship.RelationshipConstraint;
import org.neo4j.spec.graph.model.relationship.extension.RelationshipIndex;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validation;

public interface RelationshipValidation extends Validation {
    @Override
    default void validate(GraphModel model, List<Issue> issues) {
        for (var entry : model.getRelationships().entrySet()) {
            var relationshipId = entry.getKey();
            var relationship = entry.getValue();
            validateRelationship(model, relationshipId, relationship, issues);
            for (var entry2 : relationship.getProperties().entrySet()) {
                var propertyId = entry2.getKey();
                var property = entry2.getValue();
                validateProperty(model, relationshipId, relationship, propertyId, property, issues);
            }
            for (var entry2 : relationship.getConstraints().entrySet()) {
                var constraintId = entry2.getKey();
                var constraint = entry2.getValue();
                validateConstraint(model, relationshipId, relationship, constraintId, constraint, issues);
            }
            for (var entry2 : relationship.getExtensions().getIndexes().entrySet()) {
                var indexId = entry2.getKey();
                var index = entry2.getValue();
                validateIndex(model, relationshipId, relationship, indexId, index, issues);
            }
        }
    }

    default void validateRelationship(
            GraphModel model, String relationshipId, Relationship relationship, List<Issue> issues) {}

    default void validateProperty(
            GraphModel model,
            String relationshipId,
            Relationship relationship,
            String propertyId,
            Property property,
            List<Issue> issues) {}

    default void validateConstraint(
            GraphModel model,
            String relationshipId,
            Relationship relationship,
            String constraintId,
            RelationshipConstraint constraint,
            List<Issue> issues) {}

    default void validateIndex(
            GraphModel model,
            String relationshipId,
            Relationship relationship,
            String indexId,
            RelationshipIndex index,
            List<Issue> issues) {}
}
