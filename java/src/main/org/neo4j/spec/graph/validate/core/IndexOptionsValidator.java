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
import org.neo4j.spec.graph.model.IndexOptions;
import org.neo4j.spec.graph.model.IndexType;
import org.neo4j.spec.graph.model.Node;
import org.neo4j.spec.graph.model.NodeIndex;
import org.neo4j.spec.graph.model.Relationship;
import org.neo4j.spec.graph.model.RelationshipIndex;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks the type of an index's {@code options} matches the type of the index. Reports
 * {@code node_index_type_options_mismatch} and {@code relationship_index_type_options_mismatch} at the index's
 * {@code options}.
 */
public final class IndexOptionsValidator implements Validator {

    static final IndexOptionsValidator INSTANCE = new IndexOptionsValidator();

    private IndexOptionsValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        for (Map.Entry<String, Node> node : model.nodes().entrySet()) {
            node.getValue().extensions().ifPresent(extensions -> {
                for (Map.Entry<String, NodeIndex> index : extensions.indexes().entrySet()) {
                    check(
                            issues,
                            "node",
                            "nodes." + node.getKey() + ".extensions.indexes." + index.getKey(),
                            index.getValue().type(),
                            index.getValue().options());
                }
            });
        }
        for (Map.Entry<String, Relationship> relationship :
                model.relationships().entrySet()) {
            relationship.getValue().extensions().ifPresent(extensions -> {
                for (Map.Entry<String, RelationshipIndex> index :
                        extensions.indexes().entrySet()) {
                    check(
                            issues,
                            "relationship",
                            "relationships." + relationship.getKey() + ".extensions.indexes." + index.getKey(),
                            index.getValue().type(),
                            index.getValue().options());
                }
            });
        }
        return issues;
    }

    private static void check(
            List<Issue> issues, String owner, String path, IndexType type, Optional<IndexOptions> options) {
        if (options.isPresent() && options.get().type() != type) {
            issues.add(Issue.of(
                    owner + "_index_type_options_mismatch",
                    "The " + options.get().type().value() + " options don't match the index's type " + type.value(),
                    path + ".options"));
        }
    }

    @Override
    public String toString() {
        return "IndexOptionsValidator";
    }
}
