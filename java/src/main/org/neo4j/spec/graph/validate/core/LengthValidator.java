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
import java.util.Optional;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.Node;
import org.neo4j.spec.graph.model.NodeConstraint;
import org.neo4j.spec.graph.model.NodeIndex;
import org.neo4j.spec.graph.model.Property;
import org.neo4j.spec.graph.model.Relationship;
import org.neo4j.spec.graph.model.RelationshipConstraint;
import org.neo4j.spec.graph.model.RelationshipIndex;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks strings don't exceed their maximum length, counted in Unicode code points. Reports {@code name_too_long} for
 * the model's {@code name} (256), a property's {@code name} (256) and a constraint or index {@code name} (800), and
 * {@code description_too_long} for the model's {@code description} (65,536) and the {@code description} of a node,
 * relationship or property (1,024). The details hold the {@code length} and the {@code max}.
 */
public final class LengthValidator implements Validator {

    private static final int NAME = 256;
    private static final int DESCRIPTION = 65_536;
    private static final int ELEMENT_DESCRIPTION = 1_024;
    private static final int SCHEMA_NAME = 800;

    static final LengthValidator INSTANCE = new LengthValidator();

    private LengthValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        name(issues, "name", model.name(), NAME);
        description(issues, "description", model.description(), DESCRIPTION);
        for (Map.Entry<String, Node> entry : model.nodes().entrySet()) {
            String path = "nodes." + entry.getKey();
            Node node = entry.getValue();
            description(issues, path + ".description", node.description(), ELEMENT_DESCRIPTION);
            properties(issues, path, node.properties());
            for (Map.Entry<String, NodeConstraint> constraint :
                    node.constraints().entrySet()) {
                name(
                        issues,
                        path + ".constraints." + constraint.getKey() + ".name",
                        constraint.getValue().name(),
                        SCHEMA_NAME);
            }
            node.extensions().ifPresent(extensions -> {
                for (Map.Entry<String, NodeIndex> index : extensions.indexes().entrySet()) {
                    name(
                            issues,
                            path + ".extensions.indexes." + index.getKey() + ".name",
                            index.getValue().name(),
                            SCHEMA_NAME);
                }
            });
        }
        for (Map.Entry<String, Relationship> entry : model.relationships().entrySet()) {
            String path = "relationships." + entry.getKey();
            Relationship relationship = entry.getValue();
            description(issues, path + ".description", relationship.description(), ELEMENT_DESCRIPTION);
            properties(issues, path, relationship.properties());
            for (Map.Entry<String, RelationshipConstraint> constraint :
                    relationship.constraints().entrySet()) {
                name(
                        issues,
                        path + ".constraints." + constraint.getKey() + ".name",
                        constraint.getValue().name(),
                        SCHEMA_NAME);
            }
            relationship.extensions().ifPresent(extensions -> {
                for (Map.Entry<String, RelationshipIndex> index :
                        extensions.indexes().entrySet()) {
                    name(
                            issues,
                            path + ".extensions.indexes." + index.getKey() + ".name",
                            index.getValue().name(),
                            SCHEMA_NAME);
                }
            });
        }
        return issues;
    }

    private static void properties(List<Issue> issues, String owner, Map<String, Property> properties) {
        for (Map.Entry<String, Property> entry : properties.entrySet()) {
            String path = owner + ".properties." + entry.getKey();
            name(issues, path + ".name", entry.getValue().name(), NAME);
            description(issues, path + ".description", entry.getValue().description(), ELEMENT_DESCRIPTION);
        }
    }

    private static void name(List<Issue> issues, String path, Optional<String> value, int max) {
        check(issues, "name_too_long", "name", path, value, max);
    }

    private static void description(List<Issue> issues, String path, Optional<String> value, int max) {
        check(issues, "description_too_long", "description", path, value, max);
    }

    private static void check(
            List<Issue> issues, String code, String field, String path, Optional<String> value, int max) {
        if (value.isEmpty()) {
            return;
        }
        int length = Support.length(value.get());
        if (length > max) {
            issues.add(Issue.of(
                    code,
                    "The " + field + " at " + path + " is " + length + " characters, over the maximum of " + max,
                    path,
                    Map.of("length", String.valueOf(length), "max", String.valueOf(max))));
        }
    }

    @Override
    public String toString() {
        return "LengthValidator";
    }
}
