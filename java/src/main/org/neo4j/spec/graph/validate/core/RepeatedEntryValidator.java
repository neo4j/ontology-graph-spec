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
import org.neo4j.spec.graph.model.NodeMapping;
import org.neo4j.spec.graph.model.Property;
import org.neo4j.spec.graph.model.Relationship;
import org.neo4j.spec.graph.model.RelationshipConstraint;
import org.neo4j.spec.graph.model.RelationshipIndex;
import org.neo4j.spec.graph.model.RelationshipMapping;
import org.neo4j.spec.graph.model.Table;
import org.neo4j.spec.graph.model.TableColumn;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks lists don't repeat an entry: aliases, labels, index and constraint properties, mapping keys and the
 * supported types of table columns. Reports {@code duplicate_*} issues, such as {@code duplicate_node_alias}, at the
 * repeated entry (for example {@code nodes.person.aliases[1]}) with its {@code value} in the details.
 */
public final class RepeatedEntryValidator implements Validator {

    static final RepeatedEntryValidator INSTANCE = new RepeatedEntryValidator();

    private RepeatedEntryValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        for (Map.Entry<String, Node> entry : model.nodes().entrySet()) {
            node(issues, "nodes." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Relationship> entry : model.relationships().entrySet()) {
            relationship(issues, "relationships." + entry.getKey(), entry.getValue());
        }
        model.extensions().ifPresent(extensions -> {
            for (Map.Entry<String, Table> table : extensions.dataSourceTables().entrySet()) {
                List<TableColumn> columns = table.getValue().columns();
                for (int i = 0; i < columns.size(); i++) {
                    Support.repeats(
                            issues,
                            "duplicate_table_column_supported_type",
                            "supported type",
                            "extensions.data_source_tables." + table.getKey() + ".columns[" + i + "].supported",
                            columns.get(i).supported());
                }
            }
            for (Map.Entry<String, List<Mapping>> entry :
                    extensions.dataSourceMappings().entrySet()) {
                List<Mapping> mappings = entry.getValue();
                for (int i = 0; i < mappings.size(); i++) {
                    String path = "extensions.data_source_mappings." + entry.getKey() + "[" + i + "].key";
                    Mapping mapping = mappings.get(i);
                    if (mapping instanceof NodeMapping) {
                        Support.repeats(
                                issues,
                                "duplicate_node_mapping_key",
                                "mapping key",
                                path,
                                ((NodeMapping) mapping).key());
                    } else if (mapping instanceof RelationshipMapping) {
                        Support.repeats(
                                issues,
                                "duplicate_relationship_mapping_key",
                                "mapping key",
                                path,
                                ((RelationshipMapping) mapping).key());
                    }
                }
            }
        });
        return issues;
    }

    private static void node(List<Issue> issues, String path, Node node) {
        Support.repeats(issues, "duplicate_node_alias", "alias", path + ".aliases", node.aliases());
        node.labels().ifPresent(labels -> {
            Support.repeats(
                    issues,
                    "duplicate_node_implied_label",
                    "implied label",
                    path + ".labels.implied",
                    labels.implied());
            Support.repeats(
                    issues,
                    "duplicate_node_optional_label",
                    "optional label",
                    path + ".labels.optional",
                    labels.optional());
        });
        for (Map.Entry<String, Property> property : node.properties().entrySet()) {
            Support.repeats(
                    issues,
                    "duplicate_node_property_alias",
                    "alias",
                    path + ".properties." + property.getKey() + ".aliases",
                    property.getValue().aliases());
        }
        for (Map.Entry<String, NodeConstraint> constraint : node.constraints().entrySet()) {
            Support.repeats(
                    issues,
                    "duplicate_node_constraint_property",
                    "property",
                    path + ".constraints." + constraint.getKey() + ".properties",
                    constraint.getValue().properties());
        }
        node.extensions().ifPresent(extensions -> {
            for (Map.Entry<String, NodeIndex> index : extensions.indexes().entrySet()) {
                String indexPath = path + ".extensions.indexes." + index.getKey();
                Support.repeats(
                        issues,
                        "duplicate_node_index_label",
                        "label",
                        indexPath + ".labels",
                        index.getValue().labels());
                Support.repeats(
                        issues,
                        "duplicate_node_index_property",
                        "property",
                        indexPath + ".properties",
                        index.getValue().properties());
            }
        });
    }

    private static void relationship(List<Issue> issues, String path, Relationship relationship) {
        Support.repeats(issues, "duplicate_relationship_alias", "alias", path + ".aliases", relationship.aliases());
        for (Map.Entry<String, Property> property : relationship.properties().entrySet()) {
            Support.repeats(
                    issues,
                    "duplicate_relationship_property_alias",
                    "alias",
                    path + ".properties." + property.getKey() + ".aliases",
                    property.getValue().aliases());
        }
        for (Map.Entry<String, RelationshipConstraint> constraint :
                relationship.constraints().entrySet()) {
            Support.repeats(
                    issues,
                    "duplicate_relationship_constraint_property",
                    "property",
                    path + ".constraints." + constraint.getKey() + ".properties",
                    constraint.getValue().properties());
        }
        relationship.extensions().ifPresent(extensions -> {
            for (Map.Entry<String, RelationshipIndex> index :
                    extensions.indexes().entrySet()) {
                Support.repeats(
                        issues,
                        "duplicate_relationship_index_property",
                        "property",
                        path + ".extensions.indexes." + index.getKey() + ".properties",
                        index.getValue().properties());
            }
        });
    }

    @Override
    public String toString() {
        return "RepeatedEntryValidator";
    }
}
