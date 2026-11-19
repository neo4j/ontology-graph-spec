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
import java.util.List;
import java.util.Set;
import org.neo4j.spec.graph.internal.validate.Schema;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks every constraint and index has a name, which is neither absent nor blank. Reports
 * {@code unset_node_constraint_name}, {@code unset_relationship_constraint_name}, {@code unset_node_index_name} and
 * {@code unset_relationship_index_name} at the {@code name}.
 */
public final class SchemaNameValidator implements Validator {

    static final SchemaNameValidator INSTANCE = new SchemaNameValidator();

    private SchemaNameValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        for (Schema.Element element : Schema.elements(model)) {
            if (!element.name().isPresent()) {
                String kind = element instanceof Schema.Constraint ? "constraint" : "index";
                issues.add(Issue.of(
                        "unset_" + element.owner() + "_" + kind + "_name",
                        "The " + kind + " '" + element.id() + "' of " + element.owner() + " '" + element.ownerId()
                                + "' has no name",
                        element.path() + ".name"));
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
        return "SchemaNameValidator";
    }
}
