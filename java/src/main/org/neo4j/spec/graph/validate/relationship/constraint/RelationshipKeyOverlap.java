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
package org.neo4j.spec.graph.validate.relationship.constraint;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.model.type.ConstraintType;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.relationship.RelationshipValidation;

public final class RelationshipKeyOverlap implements RelationshipValidation {
    public static final RelationshipKeyOverlap INSTANCE = new RelationshipKeyOverlap();

    private RelationshipKeyOverlap() {}

    @Override
    public void validateRelationship(
            GraphModel model, String relationshipId, Relationship relationship, List<Issue> issues) {
        Set<Set<String>> keyPropertySets = new LinkedHashSet<>();
        for (var keyConstraint : relationship.getConstraints().values()) {
            if (keyConstraint.getType() == ConstraintType.KEY) {
                keyPropertySets.add(new LinkedHashSet<>(keyConstraint.getProperties()));
            }
        }
        if (keyPropertySets.isEmpty()) {
            return;
        }
        for (var entry : relationship.getConstraints().entrySet()) {
            var constraintId = entry.getKey();
            var constraint = entry.getValue();
            if (constraint.getType() != ConstraintType.UNIQUE && constraint.getType() != ConstraintType.EXISTS) {
                continue;
            }
            if (keyPropertySets.contains(new LinkedHashSet<>(constraint.getProperties()))) {
                issues.add(new Issue(
                        "redundant_relationship_constraint_key_overlap",
                        "Constraint '" + constraintId + "' (" + constraint.getType() + ") is redundant - "
                                + ("a KEY constraint already covers the same properties on relationship '"
                                        + relationshipId + "'"),
                        "relationships." + relationshipId + ".constraints." + constraintId));
            }
        }
    }
}
