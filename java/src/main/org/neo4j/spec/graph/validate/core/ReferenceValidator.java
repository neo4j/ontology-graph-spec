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
import org.neo4j.spec.graph.model.Mapping;
import org.neo4j.spec.graph.model.Node;
import org.neo4j.spec.graph.model.NodeConstraint;
import org.neo4j.spec.graph.model.NodeIndex;
import org.neo4j.spec.graph.model.Property;
import org.neo4j.spec.graph.model.Relationship;
import org.neo4j.spec.graph.model.RelationshipConstraint;
import org.neo4j.spec.graph.model.RelationshipIndex;
import org.neo4j.spec.graph.model.RelationshipMapping;
import org.neo4j.spec.graph.model.RelationshipTarget;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks references to other parts of the model resolve: the nodes at the ends of relationships, the properties of
 * constraints and indexes, and the key of relationship mappings. Reports {@code missing_relationship_from_node},
 * {@code missing_relationship_to_node}, {@code missing_node_constraint_property},
 * {@code missing_relationship_constraint_property}, {@code missing_node_index_property},
 * {@code missing_relationship_index_property} and {@code invalid_relationship_mapping_key}.
 */
public final class ReferenceValidator implements Validator {

    static final ReferenceValidator INSTANCE = new ReferenceValidator();

    private ReferenceValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        for (Map.Entry<String, Node> entry : model.nodes().entrySet()) {
            node(issues, "nodes." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Relationship> entry : model.relationships().entrySet()) {
            relationship(issues, model, "relationships." + entry.getKey(), entry.getValue());
        }
        model.extensions().ifPresent(extensions -> {
            for (Map.Entry<String, List<Mapping>> entry :
                    extensions.dataSourceMappings().entrySet()) {
                List<Mapping> mappings = entry.getValue();
                for (int i = 0; i < mappings.size(); i++) {
                    if (mappings.get(i) instanceof RelationshipMapping) {
                        String path = "extensions.data_source_mappings." + entry.getKey() + "[" + i + "].key";
                        mappingKey(issues, model, path, (RelationshipMapping) mappings.get(i));
                    }
                }
            }
        });
        return issues;
    }

    private static void node(List<Issue> issues, String path, Node node) {
        for (Map.Entry<String, NodeConstraint> entry : node.constraints().entrySet()) {
            properties(
                    issues,
                    "missing_node_constraint_property",
                    "constraint '" + entry.getKey() + "'",
                    path + ".constraints." + entry.getKey(),
                    entry.getValue().properties(),
                    node.properties());
        }
        node.extensions().ifPresent(extensions -> {
            for (Map.Entry<String, NodeIndex> entry : extensions.indexes().entrySet()) {
                properties(
                        issues,
                        "missing_node_index_property",
                        "index '" + entry.getKey() + "'",
                        path + ".extensions.indexes." + entry.getKey(),
                        entry.getValue().properties(),
                        node.properties());
            }
        });
    }

    private static void relationship(List<Issue> issues, GraphModel model, String path, Relationship relationship) {
        end(issues, model, "missing_relationship_from_node", path + ".from", relationship.from());
        end(issues, model, "missing_relationship_to_node", path + ".to", relationship.to());
        for (Map.Entry<String, RelationshipConstraint> entry :
                relationship.constraints().entrySet()) {
            properties(
                    issues,
                    "missing_relationship_constraint_property",
                    "constraint '" + entry.getKey() + "'",
                    path + ".constraints." + entry.getKey(),
                    entry.getValue().properties(),
                    relationship.properties());
        }
        relationship.extensions().ifPresent(extensions -> {
            for (Map.Entry<String, RelationshipIndex> entry :
                    extensions.indexes().entrySet()) {
                properties(
                        issues,
                        "missing_relationship_index_property",
                        "index '" + entry.getKey() + "'",
                        path + ".extensions.indexes." + entry.getKey(),
                        entry.getValue().properties(),
                        relationship.properties());
            }
        });
    }

    private static void end(List<Issue> issues, GraphModel model, String code, String path, RelationshipTarget target) {
        target.node().ifPresent(node -> {
            if (!model.nodes().containsKey(node)) {
                issues.add(Issue.of(code, "The node '" + node + "' doesn't exist", path + "." + node));
            }
        });
    }

    private static void properties(
            List<Issue> issues,
            String code,
            String owner,
            String path,
            List<String> referenced,
            Map<String, Property> available) {
        for (String property : referenced) {
            if (!available.containsKey(property)) {
                issues.add(Issue.of(
                        code,
                        "The " + owner + " refers to the property '" + property + "', which doesn't exist",
                        path + ".properties." + property));
            }
        }
    }

    private static void mappingKey(List<Issue> issues, GraphModel model, String path, RelationshipMapping mapping) {
        Relationship relationship = model.relationships().get(mapping.relationship());
        if (relationship == null) {
            return;
        }
        for (String property : mapping.key()) {
            if (!relationship.properties().containsKey(property)) {
                issues.add(Issue.of(
                        "invalid_relationship_mapping_key",
                        "The mapping key '" + property + "' isn't a property of the relationship '"
                                + mapping.relationship() + "'",
                        path,
                        Map.of("relationship", mapping.relationship(), "property", property)));
            }
        }
    }

    @Override
    public String toString() {
        return "ReferenceValidator";
    }
}
