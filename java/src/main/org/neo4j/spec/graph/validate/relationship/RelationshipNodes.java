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
import org.neo4j.spec.graph.validate.Issue;

public final class RelationshipNodes implements RelationshipValidation {
    public static final RelationshipNodes INSTANCE = new RelationshipNodes();

    private RelationshipNodes() {}

    @Override
    public void validateRelationship(
            GraphModel model, String relationshipId, Relationship relationship, List<Issue> issues) {
        if (!relationship.getFrom().getNode().isBlank()
                && !model.getNodes().containsKey(relationship.getFrom().getNode())) {
            issues.add(new Issue(
                    "missing_relationship_from_node",
                    "Missing node with id '" + relationship.getFrom().getNode() + "' for relationship '"
                            + relationshipId + "'",
                    Issue.jsonPointerOf("relationships", relationshipId, "from", "node")));
        }
        if (!relationship.getTo().getNode().isBlank()
                && !model.getNodes().containsKey(relationship.getTo().getNode())) {
            issues.add(new Issue(
                    "missing_relationship_to_node",
                    "Missing node with id '" + relationship.getTo().getNode() + "' for relationship '" + relationshipId
                            + "'",
                    Issue.jsonPointerOf("relationships", relationshipId, "to", "node")));
        }
    }
}
