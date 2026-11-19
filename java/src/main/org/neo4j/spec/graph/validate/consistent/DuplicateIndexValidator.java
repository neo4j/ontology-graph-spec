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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.spec.graph.internal.validate.Schema;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks indexes aren't duplicated: two indexes of the same type over the same set of labels (nodes only) and the same
 * properties, in the same order since the order matters in a composite index, on the same target in the database. The
 * target of a node is its identifier label, so nodes sharing one are on the same target, or the node itself when it
 * has no identifier label; the target of a relationship is its {@code type}. Reports {@code duplicate_node_index} and
 * {@code duplicate_relationship_index} at the later index, with the path of the {@code first}.
 */
public final class DuplicateIndexValidator implements Validator {

    static final DuplicateIndexValidator INSTANCE = new DuplicateIndexValidator();

    private DuplicateIndexValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        Map<List<Object>, String> first = new HashMap<>();
        for (Schema.Index index : Schema.indexes(model)) {
            List<Object> key = Arrays.asList(
                    index.owner(),
                    index.target(),
                    index.type(),
                    new HashSet<>(index.labels()),
                    new ArrayList<>(index.properties()));
            String earlier = first.putIfAbsent(key, index.path());
            if (earlier != null) {
                issues.add(Issue.of(
                        "duplicate_" + index.owner() + "_index",
                        "The " + index.type().value() + " index '" + index.id() + "' on " + index.properties()
                                + " duplicates the index at " + earlier,
                        index.path(),
                        Map.of("first", earlier)));
            }
        }
        return issues;
    }

    @Override
    public Set<Validator> dependsOn() {
        return Dependencies.core();
    }

    @Override
    public String toString() {
        return "DuplicateIndexValidator";
    }
}
