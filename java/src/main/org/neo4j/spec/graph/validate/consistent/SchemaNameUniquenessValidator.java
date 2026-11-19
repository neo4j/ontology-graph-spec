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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.spec.graph.internal.validate.Schema;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks constraint and index names are unique. Neo4j keeps them in one namespace, across all nodes and relationships,
 * so a constraint can't share a name with another constraint or with an index. Elements without a name are ignored.
 * Reports {@code duplicate_schema_name} at the {@code name} of every later occurrence, with the {@code value} and the
 * path of the {@code first} occurrence.
 */
public final class SchemaNameUniquenessValidator implements Validator {

    static final SchemaNameUniquenessValidator INSTANCE = new SchemaNameUniquenessValidator();

    private SchemaNameUniquenessValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        Map<String, String> first = new HashMap<>();
        for (Schema.Element element : Schema.elements(model)) {
            if (!element.name().isPresent()) {
                continue;
            }
            String name = element.name().get();
            String path = element.path() + ".name";
            String earlier = first.putIfAbsent(name, path);
            if (earlier != null) {
                Map<String, String> details = new LinkedHashMap<>();
                details.put("value", name);
                details.put("first", earlier);
                issues.add(Issue.of(
                        "duplicate_schema_name",
                        "The name '" + name + "' is already used at " + earlier
                                + ", and constraints and indexes must have unique names",
                        path,
                        details));
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
        return "SchemaNameUniquenessValidator";
    }
}
