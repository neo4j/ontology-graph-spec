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
import org.neo4j.spec.graph.model.ConstraintType;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.Node;
import org.neo4j.spec.graph.model.NodeConstraint;
import org.neo4j.spec.graph.model.Relationship;
import org.neo4j.spec.graph.model.RelationshipConstraint;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks {@code EXISTS} and {@code PROPERTY_TYPE} constraints cover exactly one property. Reports
 * {@code invalid_node_exist_constraint_property_count}, {@code invalid_node_type_constraint_property_count},
 * {@code invalid_relationship_exist_constraint_property_count} and
 * {@code invalid_relationship_type_constraint_property_count} at the constraint's {@code properties}.
 */
public final class ConstraintPropertyCountValidator implements Validator {

    static final ConstraintPropertyCountValidator INSTANCE = new ConstraintPropertyCountValidator();

    private ConstraintPropertyCountValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        for (Map.Entry<String, Node> node : model.nodes().entrySet()) {
            for (Map.Entry<String, NodeConstraint> entry :
                    node.getValue().constraints().entrySet()) {
                check(
                        issues,
                        "node",
                        "nodes." + node.getKey() + ".constraints." + entry.getKey(),
                        entry.getValue().type(),
                        entry.getValue().properties());
            }
        }
        for (Map.Entry<String, Relationship> relationship :
                model.relationships().entrySet()) {
            for (Map.Entry<String, RelationshipConstraint> entry :
                    relationship.getValue().constraints().entrySet()) {
                check(
                        issues,
                        "relationship",
                        "relationships." + relationship.getKey() + ".constraints." + entry.getKey(),
                        entry.getValue().type(),
                        entry.getValue().properties());
            }
        }
        return issues;
    }

    private static void check(List<Issue> issues, String owner, String path, ConstraintType type, List<String> props) {
        String kind;
        if (type == ConstraintType.EXISTS) {
            kind = "exist";
        } else if (type == ConstraintType.PROPERTY_TYPE) {
            kind = "type";
        } else {
            return;
        }
        if (props.size() != 1) {
            issues.add(Issue.of(
                    "invalid_" + owner + "_" + kind + "_constraint_property_count",
                    "A " + type.value() + " constraint must cover exactly one property but this covers " + props.size(),
                    path + ".properties"));
        }
    }

    @Override
    public String toString() {
        return "ConstraintPropertyCountValidator";
    }
}
