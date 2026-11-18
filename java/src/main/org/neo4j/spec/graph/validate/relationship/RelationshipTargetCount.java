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
// Generated from org/neo4j/spec/graph/validate/relationship/RelationshipTargetCount.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.validate.relationship;

import java.util.List;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.relationship.Relationship;
import org.neo4j.spec.graph.model.relationship.RelationshipTarget;
import org.neo4j.spec.graph.validate.Issue;

/**
 * A relationship target may define an exact `count` or a `minCount`/`maxCount` range, but not both.
 */
public final class RelationshipTargetCount implements RelationshipValidation {
    public static final RelationshipTargetCount INSTANCE = new RelationshipTargetCount();

    private RelationshipTargetCount() {}

    @Override
    public void validateRelationship(
            GraphModel model, String relationshipId, Relationship relationship, List<Issue> issues) {
        validateTarget(relationshipId, "from", relationship.getFrom(), issues);
        validateTarget(relationshipId, "to", relationship.getTo(), issues);
    }

    private void validateTarget(String relationshipId, String side, RelationshipTarget target, List<Issue> issues) {
        String path = "relationships." + relationshipId + "." + side;
        Integer count = target.getCount();
        Integer minCount = target.getMinCount();
        Integer maxCount = target.getMaxCount();
        boolean hasCount = count != null;
        boolean hasRange = minCount != null || maxCount != null;
        if (hasCount && hasRange) {
            issues.add(new Issue(
                    "relationship_count_conflict",
                    "Relationship '" + relationshipId + "' " + side + " can't set both count and min/max count",
                    path));
        }
        if (count != null && count < 0) {
            issues.add(new Issue(
                    "relationship_count_negative",
                    "Relationship '" + relationshipId + "' " + side + " count must not be negative",
                    path + ".count"));
        }
        if (minCount != null && minCount < 0) {
            issues.add(new Issue(
                    "relationship_min_count_negative",
                    "Relationship '" + relationshipId + "' " + side + " min count must not be negative",
                    path + ".min_count"));
        }
        if (minCount != null && maxCount != null && minCount > maxCount) {
            issues.add(new Issue(
                    "relationship_min_count_exceeds_max",
                    "Relationship '" + relationshipId + "' " + side + " min count is greater than max count",
                    path));
        }
    }
}
