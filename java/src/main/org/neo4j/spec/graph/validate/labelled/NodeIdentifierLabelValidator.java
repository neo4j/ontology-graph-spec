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
package org.neo4j.spec.graph.validate.labelled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.neo4j.spec.graph.internal.validate.Schema;
import org.neo4j.spec.graph.model.GraphModel;
import org.neo4j.spec.graph.model.Node;
import org.neo4j.spec.graph.validate.Issue;
import org.neo4j.spec.graph.validate.Validator;

/**
 * Checks every node has an identifier label, which is neither absent nor blank. Reports
 * {@code unset_node_identifier_label} at the node's {@code labels.identifier}.
 */
public final class NodeIdentifierLabelValidator implements Validator {

    static final NodeIdentifierLabelValidator INSTANCE = new NodeIdentifierLabelValidator();

    private NodeIdentifierLabelValidator() {}

    @Override
    public List<Issue> validate(GraphModel model) {
        List<Issue> issues = new ArrayList<>();
        for (Map.Entry<String, Node> entry : model.nodes().entrySet()) {
            if (!Schema.identifier(entry.getValue()).isPresent()) {
                issues.add(Issue.of(
                        "unset_node_identifier_label",
                        "The node '" + entry.getKey() + "' has no identifier label",
                        "nodes." + entry.getKey() + ".labels.identifier"));
            }
        }
        return issues;
    }

    @Override
    public String toString() {
        return "NodeIdentifierLabelValidator";
    }
}
