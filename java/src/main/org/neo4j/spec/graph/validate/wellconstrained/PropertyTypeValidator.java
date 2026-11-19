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
package org.neo4j.spec.graph.validate.wellconstrained;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.Node;
import org.neo4j.spec.graph.model.Property;
import org.neo4j.spec.graph.model.Relationship;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks every property of nodes and relationships has a type. A type of {@code ANY} counts as no type. Reports
 * {@code property_without_type} at the property.
 */
public final class PropertyTypeValidator implements Validator {

    static final PropertyTypeValidator INSTANCE = new PropertyTypeValidator();

    private PropertyTypeValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        for (Map.Entry<String, Node> node : model.nodes().entrySet()) {
            check(issues, "nodes." + node.getKey(), node.getValue().properties());
        }
        for (Map.Entry<String, Relationship> relationship :
                model.relationships().entrySet()) {
            check(
                    issues,
                    "relationships." + relationship.getKey(),
                    relationship.getValue().properties());
        }
        return issues;
    }

    private static void check(List<Issue> issues, String owner, Map<String, Property> properties) {
        for (Map.Entry<String, Property> property : properties.entrySet()) {
            if (!property.getValue().type().isPresent()) {
                issues.add(Issue.of(
                        "property_without_type",
                        "The property '" + property.getKey() + "' has no type",
                        owner + ".properties." + property.getKey()));
            }
        }
    }

    @Override
    public String toString() {
        return "PropertyTypeValidator";
    }
}
