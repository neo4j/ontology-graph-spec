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
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.model.relationship.RelationshipConstraint;
import org.neo4j.spec.graph.validate.Issue;

public final class RelationshipConstraints implements RelationshipValidation {
    public static final RelationshipConstraints INSTANCE = new RelationshipConstraints();

    private RelationshipConstraints() {}

    @Override
    public void validateConstraint(
            GraphModel model,
            String relationshipId,
            Relationship relationship,
            String constraintId,
            RelationshipConstraint constraint,
            List<Issue> issues) {
        for (var property : constraint.getProperties()) {
            if (!relationship.getProperties().containsKey(property)) {
                issues.add(new Issue(
                        "missing_relationship_constraint_property",
                        "Missing property with id '" + property + "' for relationship constraint '" + constraintId
                                + "'",
                        "relationships." + relationshipId + ".constraints." + constraintId + ".properties."
                                + property));
            }
        }
    }
}
