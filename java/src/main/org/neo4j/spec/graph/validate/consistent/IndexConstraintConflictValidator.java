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
package org.neo4j.spec.graph.validate.consistent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.spec.graph.internal.validate.Schema;
import org.neo4j.spec.graph.model.ConstraintType;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.IndexType;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks no {@code RANGE} index has exactly the same ordered properties as a {@code UNIQUE} or {@code KEY} constraint on
 * the same target. Neo4j backs those constraints with a range index, so creating both fails. Reports
 * {@code node_index_conflicts_with_constraint} and {@code relationship_index_conflicts_with_constraint} at the index,
 * with the path of the {@code constraint}.
 */
public final class IndexConstraintConflictValidator implements Validator {

    static final IndexConstraintConflictValidator INSTANCE = new IndexConstraintConflictValidator();

    private IndexConstraintConflictValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        Map<List<Object>, String> backed = new HashMap<>();
        for (Schema.Constraint constraint : Schema.constraints(model)) {
            if ((constraint.type() == ConstraintType.UNIQUE || constraint.type() == ConstraintType.KEY)
                    && !constraint.properties().isEmpty()) {
                backed.putIfAbsent(key(constraint), constraint.path());
            }
        }
        List<Issue> issues = new ArrayList<>();
        for (Schema.Index index : Schema.indexes(model)) {
            String constraint = index.type() == IndexType.RANGE ? backed.get(key(index)) : null;
            if (constraint != null) {
                issues.add(Issue.of(
                        index.owner() + "_index_conflicts_with_constraint",
                        "The RANGE index '" + index.id() + "' on " + index.properties()
                                + " conflicts with the constraint at " + constraint
                                + ", which is backed by an identical index",
                        index.path(),
                        Map.of("constraint", constraint)));
            }
        }
        return issues;
    }

    private static List<Object> key(Schema.Element element) {
        return Arrays.asList(element.owner(), element.target(), new ArrayList<>(element.properties()));
    }

    @Override
    public Set<Validator> dependsOn() {
        return Dependencies.core();
    }

    @Override
    public String toString() {
        return "IndexConstraintConflictValidator";
    }
}
