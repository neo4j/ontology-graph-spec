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
import org.neo4j.spec.graph.model.Node;
import org.neo4j.spec.graph.model.Property;
import org.neo4j.spec.graph.model.Relationship;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks a property doesn't set both {@code pattern} and {@code one_of}. Reports
 * {@code property_pattern_one_of_conflict} at the property.
 */
public final class PropertyPatternValidator implements Validator {

    static final PropertyPatternValidator INSTANCE = new PropertyPatternValidator();

    private PropertyPatternValidator() {}

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
        for (Map.Entry<String, Property> entry : properties.entrySet()) {
            Property property = entry.getValue();
            if (property.pattern().isPresent() && !property.oneOf().isEmpty()) {
                issues.add(Issue.of(
                        "property_pattern_one_of_conflict",
                        "The property '" + entry.getKey() + "' can't set both pattern and one_of",
                        owner + ".properties." + entry.getKey()));
            }
        }
    }

    @Override
    public String toString() {
        return "PropertyPatternValidator";
    }
}
