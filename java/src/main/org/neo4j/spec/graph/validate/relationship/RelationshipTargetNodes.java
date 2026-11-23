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
import org.neo4j.spec.graph.model.relationship.RelationshipTarget;
import org.neo4j.spec.graph.validate.Issue;

/** Every relationship must set the node it starts from and the node it points to. */
public final class RelationshipTargetNodes implements RelationshipValidation {
    public static final RelationshipTargetNodes INSTANCE = new RelationshipTargetNodes();

    private RelationshipTargetNodes() {}

    @Override
    public void validateRelationship(
            GraphModel model, String relationshipId, Relationship relationship, List<Issue> issues) {
        validateNode(relationshipId, "from", relationship.getFrom(), issues);
        validateNode(relationshipId, "to", relationship.getTo(), issues);
    }

    private static void validateNode(
            String relationshipId, String side, RelationshipTarget target, List<Issue> issues) {
        if (target.getNode().isBlank()) {
            issues.add(new Issue(
                    "missing_relationship_" + side,
                    "Missing " + side + " node for relationship '" + relationshipId + "'",
                    Issue.jsonPointerOf("relationships", relationshipId, side, "node")));
        }
    }
}
