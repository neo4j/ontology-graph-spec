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
package org.neo4j.spec.graph.validate.labelled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.Relationship;
import org.neo4j.spec.graph.model.RelationshipTarget;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks both ends of every relationship have a {@code node} set. Reports {@code unset_relationship_from_node} at the
 * relationship's {@code from.node} and {@code unset_relationship_to_node} at its {@code to.node}. Ends that name a node
 * which doesn't exist are reported by {@code ReferenceValidator} in the core set instead.
 */
public final class RelationshipEndsValidator implements Validator {

    static final RelationshipEndsValidator INSTANCE = new RelationshipEndsValidator();

    private RelationshipEndsValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        for (Map.Entry<String, Relationship> entry : model.relationships().entrySet()) {
            String path = "relationships." + entry.getKey();
            end(
                    issues,
                    "from",
                    path + ".from.node",
                    entry.getKey(),
                    entry.getValue().from());
            end(
                    issues,
                    "to",
                    path + ".to.node",
                    entry.getKey(),
                    entry.getValue().to());
        }
        return issues;
    }

    private static void end(List<Issue> issues, String end, String path, String id, RelationshipTarget target) {
        if (!target.node().isPresent()) {
            issues.add(Issue.of(
                    "unset_relationship_" + end + "_node",
                    "The relationship '" + id + "' has no " + end + " node",
                    path));
        }
    }

    @Override
    public String toString() {
        return "RelationshipEndsValidator";
    }
}
