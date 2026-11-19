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
package org.neo4j.spec.graph.validate.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.Relationship;
import org.neo4j.spec.graph.model.RelationshipTarget;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks the counts of relationship ends: {@code count} can't be set with {@code min_count} or {@code max_count},
 * {@code count} and {@code min_count} can't be negative, and {@code min_count} can't exceed {@code max_count}.
 * Reports {@code relationship_count_conflict} and {@code relationship_min_count_exceeds_max} at the end, and
 * {@code relationship_count_negative} and {@code relationship_min_count_negative} at the count.
 */
public final class RelationshipCountValidator implements Validator {

    static final RelationshipCountValidator INSTANCE = new RelationshipCountValidator();

    private RelationshipCountValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        for (Map.Entry<String, Relationship> entry : model.relationships().entrySet()) {
            String path = "relationships." + entry.getKey();
            end(issues, path + ".from", entry.getValue().from());
            end(issues, path + ".to", entry.getValue().to());
        }
        return issues;
    }

    private static void end(List<Issue> issues, String path, RelationshipTarget end) {
        Integer count = end.count().orElse(null);
        Integer min = end.minCount().orElse(null);
        Integer max = end.maxCount().orElse(null);
        if (count != null && (min != null || max != null)) {
            issues.add(Issue.of(
                    "relationship_count_conflict",
                    "The relationship end can't set count together with min_count or max_count",
                    path));
        }
        if (count != null && count < 0) {
            issues.add(Issue.of(
                    "relationship_count_negative", "The count " + count + " must not be negative", path + ".count"));
        }
        if (min != null && min < 0) {
            issues.add(Issue.of(
                    "relationship_min_count_negative",
                    "The min_count " + min + " must not be negative",
                    path + ".min_count"));
        }
        if (min != null && max != null && min > max) {
            issues.add(Issue.of(
                    "relationship_min_count_exceeds_max",
                    "The min_count " + min + " must not be greater than the max_count " + max,
                    path));
        }
    }

    @Override
    public String toString() {
        return "RelationshipCountValidator";
    }
}
